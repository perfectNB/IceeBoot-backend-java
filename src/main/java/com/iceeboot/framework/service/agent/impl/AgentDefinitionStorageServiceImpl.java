package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

/** 负责智能体定义数据，事务边界保持在公开服务方法。 */
@Service
@RequiredArgsConstructor
public class AgentDefinitionStorageServiceImpl implements IAgentDefinitionStorageService {
    private final AgentDefinitionMapper agents;
    private final AgentJsonCodec codec;

    private static <T> T required(T value, String message) {
        if (value == null) throw new IceeBootException(message);
        return value;
    }

    private static void changed(int affected, String message) {
        if (affected != 1) throw new IceeBootException(message);
    }

    @Override
    public List<AgentView> agents() {
        return agents.selectWithStats(null, null).stream().map(this::view).toList();
    }

    @Override
    public AgentView agent(long id) {
        return agents.selectWithStats(id, null).stream()
                .findFirst()
                .map(this::view)
                .orElseThrow(() -> new IceeBootException(ResponseMessage.Agent.AGENT_NOT_FOUND));
    }

    @Override
    public AgentView agentByCode(String code) {
        return agents.selectWithStats(null, code).stream()
                .findFirst()
                .map(this::view)
                .orElseThrow(() -> new IceeBootException(ResponseMessage.Agent.AGENT_NOT_FOUND));
    }

    @Override
    public long saveAgent(AgentSpec a) {
        AgentDefinitionDO row = new AgentDefinitionDO();
        row.setId(a.id());
        row.setCode(a.code());
        row.setName(a.name());
        row.setDescription(a.description());
        row.setEnabled(a.enabled());
        row.setAvatar(a.avatar());
        row.setPrompt(a.prompt());
        row.setModelConfigId(a.modelConfigId());
        row.setMaxRounds(a.maxRounds());
        row.setSandboxEnabled(a.sandboxEnabled());
        row.setSandboxTemplateId(a.sandboxTemplateId());
        row.setPlanMode(a.planMode());
        row.setOutputFormat(a.outputFormat().name());
        row.setOutputSchemaJson(codec.json(a.outputSchema()));
        row.setGenerationJson(codec.json(a.generation()));
        row.setSkillIdsJson(codec.json(a.skillIds()));
        row.setToolIdsJson(codec.json(a.toolIds()));
        row.setMcpIdsJson(codec.json(a.mcpIds()));
        row.setKnowledgeIdsJson(codec.json(a.knowledgeIds()));
        row.setChildAgentIdsJson(codec.json(a.childAgentIds()));
        row.setEnvironmentJson(codec.json(a.environment()));
        if (a.id() == null) {
            row.setActiveReleaseId("");
            changed(agents.insert(row), ResponseMessage.Agent.AGENT_CREATE_FAILED);
        } else {
            AgentDefinitionDO old =
                    required(agents.selectById(a.id()), ResponseMessage.Agent.AGENT_NOT_FOUND);
            if (!old.getCode().equals(a.code()))
                throw new IceeBootException(ResponseMessage.Agent.AGENT_CODE_IMMUTABLE);
            changed(agents.updateById(row), ResponseMessage.Agent.AGENT_NOT_FOUND);
        }
        return row.getId();
    }

    private AgentView view(AgentDefinitionDO r) {
        AgentSpec draft =
                new AgentSpec(
                        r.getId(),
                        r.getCode(),
                        r.getName(),
                        r.getDescription(),
                        r.getAvatar(),
                        r.getPrompt(),
                        r.getModelConfigId(),
                        r.getMaxRounds(),
                        Boolean.TRUE.equals(r.getEnabled()),
                        Boolean.TRUE.equals(r.getSandboxEnabled()),
                        r.getSandboxTemplateId(),
                        Boolean.TRUE.equals(r.getPlanMode()),
                        OutputFormat.valueOf(r.getOutputFormat()),
                        codec.map(r.getOutputSchemaJson(), Object.class),
                        codec.read(r.getGenerationJson(), Generation.class),
                        codec.list(r.getSkillIdsJson(), Long.class),
                        codec.list(r.getToolIdsJson(), Long.class),
                        codec.list(r.getMcpIdsJson(), Long.class),
                        codec.list(r.getKnowledgeIdsJson(), Long.class),
                        codec.list(r.getChildAgentIdsJson(), Long.class),
                        codec.map(r.getEnvironmentJson(), String.class));
        return new AgentView(draft, r.getActiveReleaseId(), r.getActiveVersion(), r.getCallCount());
    }

    @Override
    public void deleteAgent(long id) {
        agents.deleteById(id);
    }

    @Override
    public void lockAgent(long id) {
        required(agents.lockById(id), ResponseMessage.Agent.AGENT_NOT_FOUND);
    }
}
