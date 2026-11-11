package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.support.*;
import com.iceeboot.framework.vo.agent.*;
import jakarta.validation.Validation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentSkillImportServiceImpl implements IAgentSkillImportService {
    private final SkillArchiveParser parser;
    private final GithubSkillSource github;
    private final IAgentSkillService skills;

    @Override
    public AgentSkillImportPreviewVO previewArchive(AgentSkillArchiveDTO request) {
        var file=request.getFile();
        if(file==null || file.isEmpty() || file.getSize()>SkillArchiveParser.MAX_ARCHIVE_BYTES)
            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
        try (var input=file.getInputStream()) {
            return parser.parse(SkillArchiveParser.readBounded(input,SkillArchiveParser.MAX_ARCHIVE_BYTES),
                    Objects.toString(file.getOriginalFilename(),"skill.zip"),null,false);
        } catch (IceeBootException e) { throw e; }
        catch (java.io.IOException e) { throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_ARCHIVE,e); }
    }
    @Override
    public AgentSkillImportPreviewVO previewGithub(AgentSkillGithubDTO request) {
        var archive=github.download(request);
        return parser.parse(archive.bytes(),"github.zip",archive.directory(),true);
    }
    @Override
    @Transactional(rollbackFor=Exception.class)
    public List<AgentSkillVO> commit(AgentSkillImportDTO request) {
        try (var factory=Validation.buildDefaultValidatorFactory()) {
            if(!factory.getValidator().validate(request).isEmpty())
                throw new IceeBootException(ResponseMessage.Agent.REQUEST_INVALID);
        }
        long total=0;
        Set<String> codes=new HashSet<>();
        for(var skill:request.getSkills()) {
            if(!codes.add(skill.getCode().toLowerCase(Locale.ROOT)))
                throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_DUPLICATE);
            total+=skill.getContent().length();
            if(skill.getResources()!=null) {
                for(var entry:skill.getResources().entrySet()) {
                    if(entry.getKey()==null || entry.getValue()==null)
                        throw new IceeBootException(ResponseMessage.Agent.REQUEST_INVALID);
                    total+=entry.getValue().length();
                }
            }
            if(total>SkillArchiveParser.MAX_EXPANDED_BYTES) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
        }
        List<AgentSkillVO> result=new ArrayList<>();
        for(var skill:request.getSkills()) result.add(skills.addResource(skill));
        return result;
    }
}
