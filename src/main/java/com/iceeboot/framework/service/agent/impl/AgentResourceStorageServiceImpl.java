package com.iceeboot.framework.service.agent.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.IceeBootDO;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 负责资源注册和明细数据，事务边界保持在公开服务方法。 */
@Service
@RequiredArgsConstructor
public class AgentResourceStorageServiceImpl implements IAgentResourceStorageService {
    private final AgentCapabilityMapper capabilities;
    private final AgentSkillMapper skills;
    private final AgentToolMapper tools;
    private final AgentMcpMapper mcps;
    private final AgentSandboxTemplateMapper sandboxes;
    private final AgentKnowledgeMapper knowledge;
    private final AgentJsonCodec codec;

    private static <T> T required(T value, String message) {
        if (value == null) throw new IceeBootException(message);
        return value;
    }

    private static void changed(int affected, String message) {
        if (affected != 1) throw new IceeBootException(message);
    }

    @Override
    public List<Capability> capabilities() {
        return capabilities(null);
    }

    @Override
    public List<Capability> capabilities(CapabilityKind kind) {
        List<AgentCapabilityDO> registry =
                capabilities.selectList(
                        new LambdaQueryWrapper<AgentCapabilityDO>()
                                .eq(
                                        kind != null,
                                        AgentCapabilityDO::getKind,
                                        kind == null ? null : kind.name())
                                .orderByDesc(AgentCapabilityDO::getId)
                                .last("LIMIT 1000"));
        Map<Long, Capability> result = new HashMap<>();
        List<Long> skillsIds =
                registry.stream()
                        .filter(r -> "SKILL".equals(r.getKind()))
                        .map(AgentCapabilityDO::getId)
                        .toList();
        if (!skillsIds.isEmpty())
            skills.selectBatchIds(skillsIds).forEach(r -> result.put(r.getId(), skill(r)));
        List<Long> toolsIds =
                registry.stream()
                        .filter(r -> "TOOL".equals(r.getKind()))
                        .map(AgentCapabilityDO::getId)
                        .toList();
        if (!toolsIds.isEmpty())
            tools.selectBatchIds(toolsIds).forEach(r -> result.put(r.getId(), tool(r)));
        List<Long> mcpsIds =
                registry.stream()
                        .filter(r -> "MCP".equals(r.getKind()))
                        .map(AgentCapabilityDO::getId)
                        .toList();
        if (!mcpsIds.isEmpty())
            mcps.selectBatchIds(mcpsIds).forEach(r -> result.put(r.getId(), mcp(r)));
        List<Long> sandboxesIds =
                registry.stream()
                        .filter(r -> "SANDBOX".equals(r.getKind()))
                        .map(AgentCapabilityDO::getId)
                        .toList();
        if (!sandboxesIds.isEmpty())
            sandboxes.selectBatchIds(sandboxesIds).forEach(r -> result.put(r.getId(), sandbox(r)));
        List<Long> knowledgeIds =
                registry.stream()
                        .filter(r -> "KNOWLEDGE".equals(r.getKind()))
                        .map(AgentCapabilityDO::getId)
                        .toList();
        if (!knowledgeIds.isEmpty())
            knowledge
                    .selectBatchIds(knowledgeIds)
                    .forEach(r -> result.put(r.getId(), knowledge(r)));
        return registry.stream()
                .map(
                        r ->
                                required(
                                        result.get(r.getId()),
                                        ResponseMessage.Agent.RESOURCE_DETAIL_MISSING.formatted(
                                                r.getCode())))
                .toList();
    }

    @Override
    public Capability capability(long id) {
        AgentCapabilityDO registry =
                required(capabilities.selectById(id), ResponseMessage.Agent.CAPABILITY_NOT_FOUND);
        return switch (CapabilityKind.valueOf(registry.getKind())) {
            case SKILL ->
                    skill(
                            required(
                                    skills.selectById(id),
                                    ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND));
            case TOOL ->
                    tool(
                            required(
                                    tools.selectById(id),
                                    ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND));
            case MCP ->
                    mcp(
                            required(
                                    mcps.selectById(id),
                                    ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND));
            case SANDBOX ->
                    sandbox(
                            required(
                                    sandboxes.selectById(id),
                                    ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND));
            case KNOWLEDGE ->
                    knowledge(
                            required(
                                    knowledge.selectById(id),
                                    ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND));
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveCapability(Capability c) {
        AgentCapabilityDO registry = new AgentCapabilityDO();
        registry.setId(c.id());
        registry.setKind(c.kind().name());
        registry.setCode(c.code());
        if (c.id() == null)
            changed(capabilities.insert(registry), ResponseMessage.Agent.CAPABILITY_CREATE_FAILED);
        else {
            AgentCapabilityDO old =
                    required(
                            capabilities.selectById(c.id()),
                            ResponseMessage.Agent.CAPABILITY_NOT_FOUND);
            if (!old.getKind().equals(c.kind().name()) || !old.getCode().equals(c.code()))
                throw new IceeBootException(ResponseMessage.Agent.CAPABILITY_IDENTITY_IMMUTABLE);
        }
        long id = registry.getId();
        switch (c.kind()) {
            case SKILL -> {
                AgentSkillDO row = new AgentSkillDO();
                row.setId(id);
                row.setCode(c.code());
                row.setName(c.name());
                row.setDescription(c.description());
                row.setEnabled(c.enabled());
                row.setContent(c.skill().content());
                row.setResourcesJson(codec.json(c.skill().resources()));
                row.setToolIdsJson(codec.json(c.skill().toolIds()));
                saveDetail(skills, row, c.id() == null);
            }
            case TOOL -> {
                AgentToolDO row = new AgentToolDO();
                row.setId(id);
                row.setCode(c.code());
                row.setName(c.name());
                row.setDescription(c.description());
                row.setEnabled(c.enabled());
                row.setKind(c.tool().kind().name());
                row.setUrl(c.tool().url());
                row.setMethod(c.tool().method());
                row.setHeadersJson(codec.json(c.tool().headers()));
                row.setInputSchemaJson(codec.json(c.tool().inputSchema()));
                row.setScript(c.tool().script());
                row.setProjectApi(c.tool().projectApi());
                row.setPermissionCode(c.tool().permissionCode());
                saveDetail(tools, row, c.id() == null);
            }
            case MCP -> {
                AgentMcpDO row = new AgentMcpDO();
                row.setId(id);
                row.setCode(c.code());
                row.setName(c.name());
                row.setDescription(c.description());
                row.setEnabled(c.enabled());
                row.setUrl(c.mcp().url());
                row.setHeadersJson(codec.json(c.mcp().headers()));
                row.setAllowedToolsJson(codec.json(c.mcp().allowedTools()));
                saveDetail(mcps, row, c.id() == null);
            }
            case SANDBOX -> {
                AgentSandboxTemplateDO row = new AgentSandboxTemplateDO();
                row.setId(id);
                row.setCode(c.code());
                row.setName(c.name());
                row.setDescription(c.description());
                row.setEnabled(c.enabled());
                row.setTemplateId(c.sandbox().templateId());
                row.setTimeoutSeconds(c.sandbox().timeoutSeconds());
                saveDetail(sandboxes, row, c.id() == null);
            }
            case KNOWLEDGE -> {
                AgentKnowledgeDO row = new AgentKnowledgeDO();
                row.setId(id);
                row.setCode(c.code());
                row.setName(c.name());
                row.setDescription(c.description());
                row.setEnabled(c.enabled());
                row.setUrl(c.tool().url());
                row.setMethod(c.tool().method());
                row.setHeadersJson(codec.json(c.tool().headers()));
                row.setInputSchemaJson(codec.json(c.tool().inputSchema()));
                row.setProjectApi(c.tool().projectApi());
                row.setPermissionCode(c.tool().permissionCode());
                saveDetail(knowledge, row, c.id() == null);
            }
        }
        return id;
    }

    private <T extends IceeBootDO> void saveDetail(BaseMapper<T> mapper, T row, boolean create) {
        changed(
                create ? mapper.insert(row) : mapper.updateById(row),
                ResponseMessage.Agent.RESOURCE_DETAIL_SAVE_FAILED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCapability(long id) {
        AgentCapabilityDO registry =
                required(capabilities.selectById(id), ResponseMessage.Agent.CAPABILITY_NOT_FOUND);
        switch (CapabilityKind.valueOf(registry.getKind())) {
            case SKILL -> skills.deleteById(id);
            case TOOL -> tools.deleteById(id);
            case MCP -> mcps.deleteById(id);
            case SANDBOX -> sandboxes.deleteById(id);
            case KNOWLEDGE -> knowledge.deleteById(id);
        }
        capabilities.deleteById(id);
    }

    private Capability skill(AgentSkillDO r) {
        return new Capability(
                r.getId(),
                CapabilityKind.SKILL,
                r.getCode(),
                r.getName(),
                r.getDescription(),
                Boolean.TRUE.equals(r.getEnabled()),
                new SkillConfig(
                        r.getContent(),
                        codec.map(r.getResourcesJson(), String.class),
                        codec.list(r.getToolIdsJson(), Long.class)),
                null,
                null,
                null);
    }

    private Capability tool(AgentToolDO r) {
        return new Capability(
                r.getId(),
                CapabilityKind.TOOL,
                r.getCode(),
                r.getName(),
                r.getDescription(),
                Boolean.TRUE.equals(r.getEnabled()),
                null,
                new ToolConfig(
                        ToolKind.valueOf(r.getKind()),
                        r.getUrl(),
                        r.getMethod(),
                        codec.map(r.getHeadersJson(), String.class),
                        codec.map(r.getInputSchemaJson(), Object.class),
                        r.getScript(),
                        Boolean.TRUE.equals(r.getProjectApi()),
                        r.getPermissionCode()),
                null,
                null);
    }

    private Capability mcp(AgentMcpDO r) {
        return new Capability(
                r.getId(),
                CapabilityKind.MCP,
                r.getCode(),
                r.getName(),
                r.getDescription(),
                Boolean.TRUE.equals(r.getEnabled()),
                null,
                null,
                new McpConfig(
                        r.getUrl(),
                        codec.map(r.getHeadersJson(), String.class),
                        codec.list(r.getAllowedToolsJson(), String.class)),
                null);
    }

    private Capability sandbox(AgentSandboxTemplateDO r) {
        return new Capability(
                r.getId(),
                CapabilityKind.SANDBOX,
                r.getCode(),
                r.getName(),
                r.getDescription(),
                Boolean.TRUE.equals(r.getEnabled()),
                null,
                null,
                null,
                new SandboxConfig(r.getTemplateId(), r.getTimeoutSeconds()));
    }

    private Capability knowledge(AgentKnowledgeDO r) {
        return new Capability(
                r.getId(),
                CapabilityKind.KNOWLEDGE,
                r.getCode(),
                r.getName(),
                r.getDescription(),
                Boolean.TRUE.equals(r.getEnabled()),
                null,
                new ToolConfig(
                        ToolKind.HTTP,
                        r.getUrl(),
                        r.getMethod(),
                        codec.map(r.getHeadersJson(), String.class),
                        codec.map(r.getInputSchemaJson(), Object.class),
                        null,
                        Boolean.TRUE.equals(r.getProjectApi()),
                        r.getPermissionCode()),
                null,
                null);
    }
}
