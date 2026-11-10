package com.iceeboot.framework.service.agent.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.IceeBootDO;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** 智能体持久化实现。资源按类型落表，发布快照与动态参数保留 JSON。 */
@Service
@RequiredArgsConstructor
public class AgentPersistenceServiceImpl implements IAgentPersistenceService {
    private final AgentModelMapper models;
    private final AgentCapabilityMapper capabilities;
    private final AgentSkillMapper skills;
    private final AgentToolMapper tools;
    private final AgentMcpMapper mcps;
    private final AgentSandboxTemplateMapper sandboxes;
    private final AgentKnowledgeMapper knowledge;
    private final AgentDefinitionMapper agents;
    private final AgentReleaseMapper releases;
    private final AgentRunMapper runs;
    private final AgentModelCallMapper calls;
    private final ObjectMapper json;

    @Override
    public String json(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.CONFIG_SERIALIZATION_FAILED,
                    e);
        }
    }

    @Override
    public <T> T read(String value, Class<T> type) {
        if (value == null || "null".equals(value)) return null;
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(), ResponseMessage.Agent.CONFIG_CORRUPTED, e);
        }
    }

    private <T> List<T> list(String value, Class<T> type) {
        if (value == null || "null".equals(value)) return List.of();
        try {
            return json.readValue(
                    value, json.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.CONFIG_RESOURCE_LIST_CORRUPTED,
                    e);
        }
    }

    private <T> Map<String, T> map(String value, Class<T> type) {
        if (value == null || "null".equals(value)) return Map.of();
        try {
            return json.readValue(
                    value,
                    json.getTypeFactory()
                            .constructMapType(LinkedHashMap.class, String.class, type));
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.CONFIG_RESOURCE_PARAMETERS_CORRUPTED,
                    e);
        }
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IceeBootException(message);
        return value;
    }

    private static void changed(int affected, String message) {
        if (affected != 1) throw new IceeBootException(message);
    }

    @Override
    public List<ModelConfig> models() {
        return models
                .selectList(
                        new LambdaQueryWrapper<AgentModelDO>()
                                .orderByDesc(AgentModelDO::getId)
                                .last("LIMIT 1000"))
                .stream()
                .map(this::model)
                .toList();
    }

    @Override
    public ModelConfig model(long id) {
        return model(required(models.selectById(id), ResponseMessage.Agent.MODEL_NOT_FOUND));
    }

    private ModelConfig model(AgentModelDO m) {
        return new ModelConfig(
                m.getId(),
                m.getName(),
                Provider.valueOf(m.getProvider()),
                Protocol.valueOf(m.getProtocol()),
                m.getBaseUrl(),
                m.getApiKey(),
                m.getModel(),
                m.getContextWindow(),
                Boolean.TRUE.equals(m.getEnabled()),
                read(m.getGenerationJson(), Generation.class));
    }

    @Override
    public long saveModel(ModelConfig m) {
        AgentModelDO row = new AgentModelDO();
        row.setId(m.id());
        row.setName(m.name());
        row.setProvider(m.provider().name());
        row.setProtocol(m.protocol().name());
        row.setBaseUrl(m.baseUrl());
        row.setApiKey(m.apiKey());
        row.setModel(m.model());
        row.setContextWindow(m.contextWindow());
        row.setEnabled(m.enabled());
        row.setGenerationJson(json(m.generation()));
        if (m.id() == null) changed(models.insert(row), ResponseMessage.Agent.MODEL_CREATE_FAILED);
        else changed(models.updateById(row), ResponseMessage.Agent.MODEL_NOT_FOUND);
        return row.getId();
    }

    @Override
    public void deleteModel(long id) {
        models.deleteById(id);
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
                row.setResourcesJson(json(c.skill().resources()));
                row.setToolIdsJson(json(c.skill().toolIds()));
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
                row.setHeadersJson(json(c.tool().headers()));
                row.setInputSchemaJson(json(c.tool().inputSchema()));
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
                row.setHeadersJson(json(c.mcp().headers()));
                row.setAllowedToolsJson(json(c.mcp().allowedTools()));
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
                row.setHeadersJson(json(c.tool().headers()));
                row.setInputSchemaJson(json(c.tool().inputSchema()));
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
                        map(r.getResourcesJson(), String.class),
                        list(r.getToolIdsJson(), Long.class)),
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
                        map(r.getHeadersJson(), String.class),
                        map(r.getInputSchemaJson(), Object.class),
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
                        map(r.getHeadersJson(), String.class),
                        list(r.getAllowedToolsJson(), String.class)),
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
                        map(r.getHeadersJson(), String.class),
                        map(r.getInputSchemaJson(), Object.class),
                        null,
                        Boolean.TRUE.equals(r.getProjectApi()),
                        r.getPermissionCode()),
                null,
                null);
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
        row.setOutputSchemaJson(json(a.outputSchema()));
        row.setGenerationJson(json(a.generation()));
        row.setSkillIdsJson(json(a.skillIds()));
        row.setToolIdsJson(json(a.toolIds()));
        row.setMcpIdsJson(json(a.mcpIds()));
        row.setKnowledgeIdsJson(json(a.knowledgeIds()));
        row.setChildAgentIdsJson(json(a.childAgentIds()));
        row.setEnvironmentJson(json(a.environment()));
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
                        map(r.getOutputSchemaJson(), Object.class),
                        read(r.getGenerationJson(), Generation.class),
                        list(r.getSkillIdsJson(), Long.class),
                        list(r.getToolIdsJson(), Long.class),
                        list(r.getMcpIdsJson(), Long.class),
                        list(r.getKnowledgeIdsJson(), Long.class),
                        list(r.getChildAgentIdsJson(), Long.class),
                        map(r.getEnvironmentJson(), String.class));
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

    @Override
    public int nextVersion(long id) {
        return releases.nextVersion(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(PublishedAgent p, String user) {
        AgentReleaseDO row = new AgentReleaseDO();
        row.setId(p.releaseId());
        row.setAgentId(p.agent().id());
        row.setVersion(p.version());
        row.setSnapshotJson(json(p));
        row.setCreateBy(user);
        changed(releases.insert(row), ResponseMessage.Agent.RELEASE_CREATE_FAILED);
        changed(
                agents.update(
                        null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                                        AgentDefinitionDO>()
                                .eq(AgentDefinitionDO::getId, p.agent().id())
                                .set(AgentDefinitionDO::getActiveReleaseId, p.releaseId())
                                .set(AgentDefinitionDO::getUpdateBy, user)
                                .set(AgentDefinitionDO::getUpdateTime, LocalDateTime.now())),
                ResponseMessage.Agent.AGENT_NOT_FOUND);
    }

    @Override
    public PublishedAgent release(String id) {
        return read(
                required(releases.selectById(id), ResponseMessage.Agent.AGENT_NOT_PUBLISHED)
                        .getSnapshotJson(),
                PublishedAgent.class);
    }

    @Override
    public List<ReleaseView> releases(long agent) {
        return releases
                .selectList(
                        new LambdaQueryWrapper<AgentReleaseDO>()
                                .eq(AgentReleaseDO::getAgentId, agent)
                                .orderByDesc(AgentReleaseDO::getVersion)
                                .last("LIMIT 100"))
                .stream()
                .map(
                        r ->
                                new ReleaseView(
                                        r.getId(),
                                        r.getVersion(),
                                        r.getCreateTime(),
                                        r.getCreateBy()))
                .toList();
    }

    @Override
    public void startRun(
            String id, String root, String parent, PublishedAgent p, String user, String input) {
        AgentRunDO row = new AgentRunDO();
        row.setId(id);
        row.setRootRunId(root);
        row.setParentRunId(parent);
        row.setAgentId(p.agent().id());
        row.setAgentCode(p.agent().code());
        row.setVersion(p.version());
        row.setUserId(user);
        row.setStatus("RUNNING");
        row.setInputText(input);
        row.setOutputText("");
        row.setErrorText("");
        row.setInputTokens(0L);
        row.setOutputTokens(0L);
        row.setUsageKnown(true);
        row.setStartedAt(LocalDateTime.now());
        changed(runs.insert(row), ResponseMessage.Agent.RUN_CREATE_FAILED);
    }

    @Override
    public void finishRun(String id, String status, String output, String error) {
        changed(
                runs.update(
                        null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                                        AgentRunDO>()
                                .eq(AgentRunDO::getId, id)
                                .set(AgentRunDO::getStatus, status)
                                .set(AgentRunDO::getOutputText, output)
                                .set(AgentRunDO::getErrorText, error)
                                .set(AgentRunDO::getFinishedAt, LocalDateTime.now())
                                .set(AgentRunDO::getUpdateTime, LocalDateTime.now())),
                ResponseMessage.Agent.RUN_NOT_FOUND);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modelCall(
            String id,
            String run,
            String root,
            String agent,
            String model,
            String input,
            String output,
            long in,
            long out,
            boolean known) {
        AgentModelCallDO row = new AgentModelCallDO();
        row.setId(id);
        row.setRunId(run);
        row.setRootRunId(root);
        row.setAgentCode(agent);
        row.setModel(model);
        row.setInputText(input);
        row.setOutputText(output);
        row.setInputTokens(in);
        row.setOutputTokens(out);
        row.setUsageKnown(known);
        changed(calls.insert(row), ResponseMessage.Agent.MODEL_CALL_RECORD_FAILED);
        changed(runs.addUsage(run, in, out, known), ResponseMessage.Agent.RUN_NOT_FOUND);
    }

    @Override
    public List<RunView> runs() {
        return runs
                .selectList(
                        new LambdaQueryWrapper<AgentRunDO>()
                                .apply("id = root_run_id")
                                .orderByDesc(AgentRunDO::getStartedAt)
                                .last("LIMIT 100"))
                .stream()
                .map(this::run)
                .toList();
    }

    @Override
    public RunDetail runDetail(String root) {
        List<RunView> records =
                runs
                        .selectList(
                                new LambdaQueryWrapper<AgentRunDO>()
                                        .eq(AgentRunDO::getRootRunId, root)
                                        .orderByAsc(AgentRunDO::getStartedAt, AgentRunDO::getId))
                        .stream()
                        .map(this::run)
                        .toList();
        if (records.isEmpty()) throw new IceeBootException(ResponseMessage.Agent.RUN_NOT_FOUND);
        List<ModelCallView> modelCalls =
                calls
                        .selectList(
                                new LambdaQueryWrapper<AgentModelCallDO>()
                                        .eq(AgentModelCallDO::getRootRunId, root)
                                        .orderByAsc(
                                                AgentModelCallDO::getCreateTime,
                                                AgentModelCallDO::getId))
                        .stream()
                        .map(
                                r ->
                                        new ModelCallView(
                                                r.getId(),
                                                r.getRunId(),
                                                r.getRootRunId(),
                                                r.getAgentCode(),
                                                r.getModel(),
                                                r.getInputText(),
                                                r.getOutputText(),
                                                r.getInputTokens(),
                                                r.getOutputTokens(),
                                                Boolean.TRUE.equals(r.getUsageKnown()),
                                                r.getCreateTime()))
                        .toList();
        return new RunDetail(
                records,
                modelCalls,
                modelCalls.stream().mapToLong(ModelCallView::inputTokens).sum(),
                modelCalls.stream().mapToLong(ModelCallView::outputTokens).sum(),
                modelCalls.stream().allMatch(ModelCallView::usageKnown));
    }

    private RunView run(AgentRunDO r) {
        return new RunView(
                r.getId(),
                r.getRootRunId(),
                r.getParentRunId(),
                r.getAgentId(),
                r.getAgentCode(),
                r.getVersion(),
                r.getUserId(),
                r.getStatus(),
                r.getInputText(),
                r.getOutputText(),
                r.getErrorText(),
                r.getInputTokens(),
                r.getOutputTokens(),
                Boolean.TRUE.equals(r.getUsageKnown()),
                r.getStartedAt(),
                r.getFinishedAt());
    }
}
