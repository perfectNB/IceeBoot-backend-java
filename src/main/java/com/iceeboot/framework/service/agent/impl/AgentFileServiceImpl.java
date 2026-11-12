package com.iceeboot.framework.service.agent.impl;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.AgentFileKind;
import com.iceeboot.framework.mapper.agent.AgentFileMapper;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.vo.agent.AgentFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.*;

/** 文件元数据和分块内容持久化到数据库；单块 256KiB，避免 20MiB 文件触发 MySQL 单包限制。 */
@Service
@RequiredArgsConstructor
public class AgentFileServiceImpl implements IAgentFileService {
    private static final int CHUNK_BYTES=256*1024;
    private static final long MAX_SESSION_BYTES=100L*1024*1024;
    private final AgentFileMapper mapper;
    private final IAgentConversationService conversations;
    private final IAgentPersistenceService store;
    public static void validate(String name,byte[] content) {
        if(name==null || name.isBlank() || name.length()>200 || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl)
                || content==null || content.length==0 || content.length>MAX_BYTES)
            throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
    }
    private static AgentFileVO view(AgentFileDO f) {
        return new AgentFileVO(f.getId(),f.getKind(),f.getName(),f.getSizeBytes(),f.getSha256(),f.getCreateTime());
    }
    private AgentFileDO required(long agent,String user,String session,String id,boolean lock) {
        AgentFileDO row=lock ? mapper.lock(agent,user,session,id) : mapper.file(agent,user,session,id);
        if(row==null) throw new IceeBootException(404,ResponseMessage.Agent.FILE_NOT_FOUND);
        return row;
    }
    private AgentFileVO save(long agent,String user,String session,String name,byte[] content,AgentFileKind kind) {
        validate(name,content);
        mapper.lockScope(agent,user,session);
        if(mapper.storedBytes(agent,user,session)+content.length>MAX_SESSION_BYTES)
            throw new IceeBootException(ResponseMessage.Agent.FILE_SESSION_LIMIT);
        AgentFileDO row=new AgentFileDO();row.setId(UUID.randomUUID().toString());row.setAgentId(agent);
        row.setUserId(user);row.setSessionId(session);row.setName(name);row.setSizeBytes(content.length);row.setKind(kind);
        try { row.setSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))); }
        catch(java.security.NoSuchAlgorithmException failure) { throw new IceeBootException(ResponseMessage.Agent.FILE_STORAGE_FAILED,failure); }
        mapper.insert(row);
        for(int offset=0,part=0;offset<content.length;offset+=CHUNK_BYTES,part++)
            mapper.chunk(row.getId(),part,Arrays.copyOfRange(content,offset,Math.min(content.length,offset+CHUNK_BYTES)));
        return view(row);
    }
    private AgentFileContent content(AgentFileDO row) {
        var bytes=new ByteArrayOutputStream((int)row.getSizeBytes());
        for(byte[] part:mapper.chunks(row.getId())) bytes.writeBytes(part);
        if(bytes.size()!=row.getSizeBytes()) throw new IceeBootException(ResponseMessage.Agent.FILE_STORAGE_FAILED);
        return new AgentFileContent(row.getId(),row.getName(),bytes.toByteArray());
    }
    @Override @Transactional
    public AgentFileVO upload(long agent,String user,String session,String name,byte[] content) {
        AgentSessionDO conversation=conversations.require(agent,user,session);
        if(!store.release(conversation.getReleaseId()).agent().sandboxEnabled()) throw new IceeBootException(ResponseMessage.Agent.FILE_REQUIRES_SANDBOX);
        return save(agent,user,session,name,content,AgentFileKind.ATTACHMENT);
    }
    @Override
    public AgentFileContent download(long agent,String user,String session,String id) {
        conversations.require(agent,user,session);
        return content(required(agent,user,session,id,false));
    }
    @Override
    public List<AgentFileVO> list(long agent,String user,String session,String run) {
        conversations.require(agent,user,session);
        return (run==null || run.isBlank() ? mapper.pending(agent,user,session) : mapper.runFiles(agent,user,session,run)).stream().map(AgentFileServiceImpl::view).toList();
    }
    @Override @Transactional
    public void delete(long agent,String user,String session,String id) {
        conversations.require(agent,user,session);
        AgentFileDO row=required(agent,user,session,id,true);
        if(row.getKind()!=AgentFileKind.ATTACHMENT || mapper.references(id)>0) throw new IceeBootException(ResponseMessage.Agent.FILE_IN_USE);
        mapper.deleteChunks(id);mapper.delete(id);
    }
    @Override @Transactional
    public List<AgentFileContent> bind(long agent,String user,String session,String run,List<String> ids) {
        conversations.require(agent,user,session);
        if(ids==null || ids.isEmpty()) return List.of();
        if(ids.size()>5 || new HashSet<>(ids).size()!=ids.size()) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
        List<AgentFileDO> rows=ids.stream().sorted().map(id -> required(agent,user,session,id,true)).toList();
        if(rows.stream().mapToLong(AgentFileDO::getSizeBytes).sum()>MAX_BYTES) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
        var result=new ArrayList<AgentFileContent>();
        for(var row:rows) { mapper.bind(run,row.getId());result.add(content(row)); }
        return List.copyOf(result);
    }
    @Override @Transactional
    public AgentFileVO artifact(long agent,String user,String session,String run,String name,byte[] content) {
        conversations.require(agent,user,session);
        if(mapper.artifactCount(run)>=20) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
        AgentFileVO file=save(agent,user,session,name,content,AgentFileKind.ARTIFACT);
        mapper.bindOutput(run,file.id());return file;
    }
    @Override @Transactional
    public List<AgentFileVO> copyAttachments(long agent,String user,String session,String run,AgentSessionDO target) {
        conversations.require(agent,user,session);
        if(target.getAgentId()!=agent || !target.getUserId().equals(user)) throw new IceeBootException(ResponseMessage.Agent.FILE_NOT_FOUND);
        List<AgentFileVO> result=new ArrayList<>();
        for(var row:mapper.inputs(agent,user,session,run))
            result.add(save(agent,user,target.getSessionId(),row.getName(),content(row).content(),AgentFileKind.ATTACHMENT));
        return result;
    }
}
