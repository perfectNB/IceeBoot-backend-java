package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.mapper.agent.AgentConversationMapper;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.IAgentConversationService;
import io.agentscope.core.session.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 会话、展示消息和 SDK 原子日志使用同一数据库，日志保留完整工具与压缩状态。 */
@Service
@RequiredArgsConstructor
public class AgentConversationServiceImpl implements IAgentConversationService {
    private final AgentConversationMapper mapper;
    private final AgentProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final com.iceeboot.framework.service.agent.security.AgentSecrets secrets;

    @Override
    public void recordProgress(AgentProgress event) {
        try {
            mapper.insertProgress(event.rootRunId(), event.sequence(), json.writeValueAsString(event));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IceeBootException(ResponseMessage.Agent.API_CONVERSION_FAILED, e);
        }
    }

    @Override
    public List<AgentProgress> progress(long agent, String user, String session, String run) {
        require(agent, user, session);
        return mapper.progress(agent, user, session, run).stream().map(value -> {
            try { return json.readValue(value, AgentProgress.class); }
            catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IceeBootException(ResponseMessage.Agent.API_CONVERSION_FAILED, e);
            }
        }).toList();
    }


    private static void validate(String session) {
        if (session == null || !session.matches("[a-zA-Z0-9_-]{1,100}"))
            throw new IceeBootException(ResponseMessage.Agent.EXECUTION_SESSION_ID_INVALID);
    }
    @Override
    public AgentSessionDO open(long agent, String user, String session, PublishedAgent release, String title) {
        validate(session);
        AgentSessionDO row = mapper.session(agent,user,session);
        if (row != null) return row;
        row = new AgentSessionDO();
        row.setAgentId(agent);
        row.setUserId(user);
        row.setSessionId(session);
        row.setReleaseId(release.releaseId());
        row.setVersion(release.version());
        row.setTitle(title == null ? "新对话" : title.substring(0,Math.min(120,title.length())));
        try { mapper.insertSession(row);
        } catch (DuplicateKeyException race) { /* 唯一约束下读取胜出的固定版本。 */ }
        return require(agent,user,session);
    }
    @Override
    public AgentSessionDO require(long agent, String user, String session) {
        validate(session);
        AgentSessionDO row = mapper.session(agent,user,session);
        if (row == null) throw new IceeBootException(404, ResponseMessage.Agent.SESSION_NOT_FOUND);
        return row;
    }
    @Override
    public List<AgentSessionDO> sessions(long agent,String user,int offset) {
        if (offset<0) throw new IceeBootException(ResponseMessage.Agent.REQUEST_INVALID);
        return mapper.sessions(agent,user,offset);
    }
    @Override
    public List<AgentMessageDO> messages(long agent,String user,String session,long before) {
        require(agent,user,session);
        List<AgentMessageDO> rows = new ArrayList<>(mapper.messages(agent,user,session,before,System.currentTimeMillis()));
        Collections.reverse(rows);
        return rows;
    }
    @Override
    @Transactional
    public void begin(AgentSessionDO session,String run,String input) {
        long now=System.currentTimeMillis();
        if (mapper.acquire(session.getAgentId(),session.getUserId(),session.getSessionId(),run,now,
                now+(properties.getExecutionTimeoutSeconds()+60L)*1000L,input.substring(0,Math.min(120,input.length())))!=1)
            throw new IceeBootException(409,ResponseMessage.Agent.EXECUTION_SESSION_BUSY);
        mapper.interruptPending(session.getAgentId(),session.getUserId(),session.getSessionId());
        for (String role : List.of("user","assistant")) {
            AgentMessageDO message=new AgentMessageDO();
            message.setAgentId(session.getAgentId());
            message.setUserId(session.getUserId());
            message.setSessionId(session.getSessionId());
            message.setRunId(run);
            message.setRole(role);
            message.setContent(role.equals("user")?input:"");
            message.setStatus(role.equals("user")?"SUCCEEDED":"RUNNING");
            mapper.insertMessage(message);
        }
    }
    @Override
    public void finish(AgentSessionDO session,String run,String output,String status) {
        mapper.finish(session.getAgentId(),session.getUserId(),session.getSessionId(),run,output,status);
    }
    @Override
    public void release(AgentSessionDO session,String run) {
        mapper.release(session.getAgentId(),session.getUserId(),session.getSessionId(),run);
    }
    @Override
    public io.agentscope.core.state.AgentStateStore stateStore(long agent, String user, String session) {
        validate(session);
        return new io.agentscope.core.state.AgentStateStore() {
            private String slot(String sdkUser, String sdkSession, String key) {
                try {
                    byte[] source = json.writeValueAsBytes(Arrays.asList(sdkUser, sdkSession, key));
                    return "runtime/" + HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(source));
                } catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new IceeBootException(ResponseMessage.Agent.API_CONVERSION_FAILED, e);
                }
            }
            private RuntimeStateEnvelope decode(AgentSessionStateDO row) {
                if (row == null) return null;
                try {
                    String encrypted = new String(row.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                    return json.readValue(secrets.decrypt(encrypted), RuntimeStateEnvelope.class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new IceeBootException(ResponseMessage.Agent.API_CONVERSION_FAILED, e);
                }
            }
            private boolean write(RuntimeStateEnvelope value, long expected) {
                String path = slot(value.user(), value.session(), value.key());
                try {
                    byte[] payload = secrets.encrypt(json.writeValueAsString(value)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    if (expected == 0) {
                        try { return mapper.insertState(agent, user, session, path, payload) == 1; }
                        catch (DuplicateKeyException race) { return false; }
                    }
                    return mapper.updateState(agent, user, session, path, expected, payload) == 1;
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new IceeBootException(ResponseMessage.Agent.API_CONVERSION_FAILED, e);
                }
            }
            private void saveEnvelope(RuntimeStateEnvelope value) {
                for (int attempt = 0; attempt < 8; attempt++) {
                    var row = mapper.state(agent, user, session, slot(value.user(), value.session(), value.key()));
                    if (write(value, row == null ? 0 : row.getRevision())) return;
                }
                throw new IceeBootException(409, ResponseMessage.Agent.EXECUTION_SESSION_BUSY);
            }
            private List<RuntimeStateEnvelope> slots() {
                return mapper.paths(agent,user,session).stream().filter(path -> path.startsWith("runtime/"))
                        .map(path -> decode(mapper.state(agent,user,session,path))).filter(Objects::nonNull).toList();
            }
            public boolean supportsVersioning() { return true; }
            public <T extends io.agentscope.core.state.State> io.agentscope.core.state.VersionedState<T> getVersioned(String u, String s, String key, Class<T> type) {
                var row = mapper.state(agent,user,session,slot(u,s,key));
                var value = decode(row);
                T result = value == null || value.deleted() || value.list() ? null : io.agentscope.core.util.JsonUtils.getJsonCodec().fromJson(value.payload(),type);
                return new io.agentscope.core.state.VersionedState<>(result,row == null ? 0 : row.getRevision());
            }
            public long saveIfVersion(String u,String s,String key,io.agentscope.core.state.State value,long expected) {
                var envelope = new RuntimeStateEnvelope(u,s,key,false,false,io.agentscope.core.util.JsonUtils.getJsonCodec().toJson(value));
                if (!write(envelope,expected)) throw new IceeBootException(409,ResponseMessage.Agent.EXECUTION_SESSION_BUSY);
                return expected + 1;
            }
            public void save(String u,String s,String key,io.agentscope.core.state.State value) {
                saveEnvelope(new RuntimeStateEnvelope(u,s,key,false,false,io.agentscope.core.util.JsonUtils.getJsonCodec().toJson(value)));
            }
            public void save(String u,String s,String key,List<? extends io.agentscope.core.state.State> value) {
                saveEnvelope(new RuntimeStateEnvelope(u,s,key,true,false,io.agentscope.core.util.JsonUtils.getJsonCodec().toJson(value)));
            }
            public <T extends io.agentscope.core.state.State> Optional<T> get(String u,String s,String key,Class<T> type) {
                return Optional.ofNullable(getVersioned(u,s,key,type).value());
            }
            public <T extends io.agentscope.core.state.State> List<T> getList(String u,String s,String key,Class<T> type) {
                var value = decode(mapper.state(agent,user,session,slot(u,s,key)));
                if (value == null || value.deleted() || !value.list()) return List.of();
                List<?> items = io.agentscope.core.util.JsonUtils.getJsonCodec().fromJson(value.payload(),List.class);
                return items.stream().map(item -> io.agentscope.core.util.JsonUtils.getJsonCodec().convertValue(item,type)).toList();
            }
            public boolean exists(String u,String s) {
                return slots().stream().anyMatch(value -> !value.deleted() && Objects.equals(value.user(),u) && Objects.equals(value.session(),s));
            }
            public void delete(String u,String s,String key) { saveEnvelope(new RuntimeStateEnvelope(u,s,key,false,true,"")); }
            public void delete(String u,String s) {
                slots().stream().filter(value -> !value.deleted() && Objects.equals(value.user(),u) && Objects.equals(value.session(),s))
                        .forEach(value -> delete(u,s,value.key()));
            }
            public Set<String> listSessionIds(String u) {
                Set<String> result = new HashSet<>();
                slots().stream().filter(value -> !value.deleted() && Objects.equals(value.user(),u)).forEach(value -> result.add(value.session()));
                return result;
            }
        };
    }

    @Override
    public SessionLogStore logStore() { return logStore(null); }

    @Override
    public SessionLogStore logStore(com.iceeboot.framework.service.agent.runtime.RunRecorder recorder) {
        var opened = new java.util.concurrent.ConcurrentHashMap<SessionKey,SessionLog>();
        return (key,context) -> {
            long agent;
            try { agent=Long.parseLong(key.agentId());
            }
            catch (NumberFormatException e) { throw new IceeBootException(ResponseMessage.Agent.REQUEST_INVALID,e);
            }
            // 子智能体使用独立 runId 会话，所有日志仍按三元组隔离。
            long agentId=agent;
            String user=key.userId(),session=key.sessionId();
            if (!Objects.equals(user,context.getUserId())) throw new IceeBootException(ResponseMessage.Agent.PERMISSION_DENIED);
            validate(session);
            return opened.computeIfAbsent(key, ignored -> new com.iceeboot.framework.service.agent.runtime.CachedJournalSessionLog(mapper,agentId,user,session,recorder));
        };
    }
}
