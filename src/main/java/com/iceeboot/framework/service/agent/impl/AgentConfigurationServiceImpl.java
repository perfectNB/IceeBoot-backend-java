package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.framework.service.agent.memory.MemorySettingsService;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.convert.AgentModelConverter;
import com.iceeboot.framework.service.agent.runtime.SchemaSupport;
import com.iceeboot.framework.service.agent.security.AgentSecrets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

@Service
public class AgentConfigurationServiceImpl implements IAgentConfigurationService {
    private final IAgentPersistenceService store;
    private final AgentSecrets secrets;

    private final MemorySettingsService memory;
    public AgentConfigurationServiceImpl(IAgentPersistenceService store, AgentSecrets secrets,
            MemorySettingsService memory) {
        this.memory = memory;
        this.store = store;
        this.secrets = secrets;
    }

    @Override
    public List<ModelConfig> models() {
        return store.models().stream().map(this::mask).toList();
    }

    @Override
    public ModelConfig saveModel(ModelConfig m) {
        endpoint(m.baseUrl());
        if (m.provider() == Provider.CLAUDE && m.protocol() != Protocol.ANTHROPIC
                || m.provider() == Provider.GEMINI && m.protocol() != Protocol.GEMINI)
            throw new IceeBootException(ResponseMessage.Agent.MODEL_PROVIDER_PROTOCOL_MISMATCH);
        if (m.provider() != Provider.CLAUDE
                && m.provider() != Provider.GEMINI
                && m.protocol() != Protocol.OPENAI_CHAT
                && m.protocol() != Protocol.OPENAI_RESPONSES)
            throw new IceeBootException(ResponseMessage.Agent.MODEL_OPENAI_PROTOCOL_REQUIRED);
        if (m.provider() != Provider.GPT && m.protocol() == Protocol.OPENAI_RESPONSES)
            throw new IceeBootException(ResponseMessage.Agent.MODEL_RESPONSES_PROVIDER_UNSUPPORTED);
        generation(m.generation());
        String previous = m.id() == null ? null : store.model(m.id()).apiKey();
        var saved =
                new ModelConfig(
                        m.id(),
                        m.name(),
                        m.provider(),
                        m.protocol(),
                        m.baseUrl(),
                        secrets.merge(m.apiKey(), previous),
                        m.model(),
                        m.contextWindow(),
                        m.enabled(),
                        m.generation());
        if (saved.apiKey() == null || saved.apiKey().isEmpty())
            throw new IceeBootException(ResponseMessage.Agent.MODEL_API_KEY_REQUIRED);
        return mask(AgentModelConverter.withId(saved, store.saveModel(saved)));
    }

    @Override
    public List<Capability> capabilities() {
        return store.capabilities().stream().map(this::mask).toList();
    }

    @Override
    public List<Capability> capabilities(CapabilityKind kind) {
        return store.capabilities(kind).stream().map(this::mask).toList();
    }

    @Override
    public Capability capability(long id) {
        return mask(store.capability(id));
    }

    @Override
    public Capability saveCapability(Capability c) {
        if (c.kind() == CapabilityKind.TOOL || c.kind() == CapabilityKind.KNOWLEDGE) {
            require(
                    !c.code().startsWith("agent_")
                            && !Set.of(
                                            "execute",
                                            "read_file",
                                            "write_file",
                                            "edit_file",
                                            "glob",
                                            "grep",
                                            "list_files",
                                            "glob_files",
                                            "grep_files",
                                            "load_skill_through_path",
                                            "reset_tools",
                                            "generate_response",
                                            "wait_async_results",
                                            "plan_enter",
                                            "plan_write",
                                            "plan_exit",
                                            "todo_write", "deliver_artifact")
                                    .contains(c.code()),
                    ResponseMessage.Agent.TOOL_CODE_RESERVED);
        }
        Capability old = c.id() == null ? null : store.capability(c.id());
        if (old != null && (old.kind() != c.kind() || !old.code().equals(c.code())))
            throw new IceeBootException(ResponseMessage.Agent.CAPABILITY_IDENTITY_IMMUTABLE);
        SkillConfig skill = c.skill();
        ToolConfig tool = c.tool();
        McpConfig mcp = c.mcp();
        SandboxConfig sandbox = c.sandbox();
        switch (c.kind()) {
            case SKILL -> {
                require(
                        skill != null && skill.content() != null && !skill.content().isBlank(),
                        ResponseMessage.Agent.SKILL_CONTENT_REQUIRED);
                require(
                        skill.content().length() <= 100000,
                        ResponseMessage.Agent.SKILL_CONTENT_TOO_LONG);
                if (skill.resources() != null) {
                    require(
                            skill.resources().size() <= SkillConfig.MAX_RESOURCES,
                            ResponseMessage.Agent.SKILL_RESOURCES_TOO_MANY);
                    require(
                            skill.resources().values().stream()
                                            .filter(Objects::nonNull)
                                            .mapToLong(value -> value.getBytes(StandardCharsets.UTF_8).length)
                                            .sum()
                                    <= SkillConfig.MAX_TOTAL_RESOURCE_BYTES,
                            ResponseMessage.Agent.SKILL_RESOURCES_TOO_LARGE);
                    for (var e : skill.resources().entrySet()) {
                        resourcePath(e.getKey());
                        require(
                                e.getValue() != null && e.getValue().length() <= SkillConfig.MAX_RESOURCE_CHARACTERS,
                                ResponseMessage.Agent.SKILL_RESOURCE_CONTENT_TOO_LONG);
                    }
                }
                for (Long id : list(skill.toolIds()))
                    require(
                            store.capability(id).kind() == CapabilityKind.TOOL,
                            ResponseMessage.Agent.SKILL_TOOL_BINDING_INVALID);
                tool = null;
                mcp = null;
                sandbox = null;
            }
            case TOOL, KNOWLEDGE -> {
                require(
                        tool != null && tool.kind() != null,
                        ResponseMessage.Agent.TOOL_EXECUTION_CONFIG_REQUIRED);
                require(
                        tool.inputSchema() != null
                                && "object".equals(tool.inputSchema().get("type")),
                        ResponseMessage.Agent.TOOL_INPUT_SCHEMA_OBJECT_REQUIRED);
                SchemaSupport.checkSchema(tool.inputSchema());
                if (c.kind() == CapabilityKind.KNOWLEDGE)
                    require(
                            tool.kind() == ToolKind.HTTP,
                            ResponseMessage.Agent.KNOWLEDGE_HTTP_REQUIRED);
                if (tool.kind() == ToolKind.SCRIPT) {
                    require(
                            tool.script() != null
                                    && !tool.script().isBlank()
                                    && tool.script().length() <= 100000,
                            ResponseMessage.Agent.TOOL_SCRIPT_REQUIRED);
                } else if (tool.projectApi()) {
                    require(
                            tool.url() != null
                                    && tool.url().startsWith("/")
                                    && !tool.url().startsWith("//")
                                    && !tool.url().contains("..")
                                    && !tool.url().contains("\\"),
                            ResponseMessage.Agent.TOOL_PROJECT_PATH_INVALID);
                    require(
                            tool.permissionCode() != null && !tool.permissionCode().isBlank(),
                            ResponseMessage.Agent.TOOL_PROJECT_PERMISSION_REQUIRED);
                } else endpoint(tool.url());
                if (tool.kind() == ToolKind.HTTP)
                    require(
                            tool.method() != null
                                    && Set.of("GET", "POST", "PUT", "PATCH", "DELETE")
                                            .contains(tool.method()),
                            ResponseMessage.Agent.TOOL_HTTP_METHOD_UNSUPPORTED);
                tool =
                        new ToolConfig(
                                tool.kind(),
                                tool.url(),
                                tool.method(),
                                protect(
                                        tool.headers(),
                                        old == null || old.tool() == null
                                                ? null
                                                : old.tool().headers()),
                                tool.inputSchema(),
                                tool.script(),
                                tool.projectApi(),
                                tool.permissionCode());
                skill = null;
                mcp = null;
                sandbox = null;
            }
            case MCP -> {
                require(mcp != null, ResponseMessage.Agent.MCP_CONFIG_REQUIRED);
                endpoint(mcp.url());
                require(
                        mcp.allowedTools() != null
                                && !mcp.allowedTools().isEmpty()
                                && !mcp.allowedTools().contains("*"),
                        ResponseMessage.Agent.MCP_ALLOWED_TOOLS_REQUIRED);
                mcp =
                        new McpConfig(
                                mcp.url(),
                                protect(
                                        mcp.headers(),
                                        old == null || old.mcp() == null
                                                ? null
                                                : old.mcp().headers()),
                                mcp.allowedTools());
                skill = null;
                tool = null;
                sandbox = null;
            }
            case SANDBOX -> {
                require(
                        sandbox != null
                                && sandbox.templateId() != null
                                && !sandbox.templateId().isBlank(),
                        ResponseMessage.Agent.SANDBOX_TEMPLATE_REQUIRED);
                skill = null;
                tool = null;
                mcp = null;
            }
        }
        Capability saved =
                new Capability(
                        c.id(),
                        c.kind(),
                        c.code(),
                        c.name(),
                        c.description(),
                        c.enabled(),
                        skill,
                        tool,
                        mcp,
                        sandbox);
        return mask(AgentModelConverter.withId(saved, store.saveCapability(saved)));
    }

    @Override
    public List<AgentView> agents() {
        return store.agents().stream().map(this::mask).toList();
    }

    @Override
    public AgentView agent(long id) {
        return mask(store.agent(id));
    }

    @Override
    public AgentView saveAgent(AgentSpec a) {
        AgentSpec old = a.id() == null ? null : store.agent(a.id()).draft();
        if (old != null && !old.code().equals(a.code()))
            throw new IceeBootException(ResponseMessage.Agent.AGENT_CODE_IMMUTABLE);
        generation(a.generation());
        if (a.environment() != null)
            for (String key : a.environment().keySet())
                require(
                        key.matches("[a-zA-Z_][a-zA-Z0-9_]*"),
                        ResponseMessage.Agent.AGENT_ENVIRONMENT_KEY_INVALID);
        if (a.outputFormat() == OutputFormat.JSON_SCHEMA)
            require(
                    a.outputSchema() != null && "object".equals(a.outputSchema().get("type")),
                    ResponseMessage.Agent.AGENT_OUTPUT_SCHEMA_REQUIRED);
        SchemaSupport.checkSchema(a.outputSchema());
        AgentSpec saved =
                environment(a, protect(a.environment(), old == null ? null : old.environment()));
        long id = store.saveAgent(saved);
        return agent(id);
    }

    @Transactional
    @Override
    public ReleaseView publish(long id, String user) {
        store.lockAgent(id);
        PublishedAgent preview = preview(id);
        String releaseId = UUID.randomUUID().toString();
        PublishedAgent published = new PublishedAgent(releaseId,store.nextVersion(id),preview.agent(),
                preview.model(),preview.capabilities(),preview.children(), memory.releaseRules(id));
        store.publish(published,user);
        return store.releases(id).stream().filter(r -> r.id().equals(releaseId)).findFirst().orElseThrow();
    }

    @Transactional
    @Override
    public ReleaseView rollback(long id,String releaseId,String user) {
        store.lockAgent(id);
        PublishedAgent previous = store.release(releaseId);
        require(Objects.equals(previous.agent().id(),id),ResponseMessage.Agent.RELEASE_AGENT_MISMATCH);
        String nextId = UUID.randomUUID().toString();
        store.publish(new PublishedAgent(nextId,store.nextVersion(id),previous.agent(),previous.model(),
                previous.capabilities(),previous.children(),previous.memoryRules()),user);
        return store.releases(id).stream().filter(r -> r.id().equals(nextId)).findFirst().orElseThrow();
    }

    @Override
    public PublishedAgent preview(long id) {
        AgentSpec a = store.agent(id).draft();
        SchemaSupport.checkSchema(a.outputSchema());
        ModelConfig model = store.model(a.modelConfigId());
        require(model.enabled(), ResponseMessage.Agent.MODEL_DISABLED);
        LinkedHashMap<Long, Capability> capabilities = new LinkedHashMap<>();
        bind(capabilities, a.skillIds(), CapabilityKind.SKILL);
        bind(capabilities, a.toolIds(), CapabilityKind.TOOL);
        bind(capabilities, a.mcpIds(), CapabilityKind.MCP);
        bind(capabilities, a.knowledgeIds(), CapabilityKind.KNOWLEDGE);
        // Skill 关联工具属于发布快照的一部分，绝不在调用期查询数据库增加能力。
        for (Capability c : List.copyOf(capabilities.values()))
            if (c.kind() == CapabilityKind.SKILL)
                bind(capabilities, c.skill().toolIds(), CapabilityKind.TOOL);
        if (a.sandboxEnabled()) {
            require(
                    a.sandboxTemplateId() != null,
                    ResponseMessage.Agent.SANDBOX_TEMPLATE_NOT_SELECTED);
            bind(capabilities, List.of(a.sandboxTemplateId()), CapabilityKind.SANDBOX);
        }
        if (!a.sandboxEnabled())
            for (Capability c : capabilities.values())
                require(
                        c.tool() == null || c.tool().kind() != ToolKind.SCRIPT,
                        ResponseMessage.Agent.TOOL_SCRIPT_REQUIRES_SANDBOX);
        List<PublishedAgent> children = new ArrayList<>();
        for (Long child : list(a.childAgentIds())) {
            require(child != id, ResponseMessage.Agent.AGENT_CHILD_SELF_REFERENCE);
            AgentView v = store.agent(child);
            require(v.draft().enabled(), ResponseMessage.Agent.AGENT_CHILD_DISABLED);
            PublishedAgent p = store.release(v.activeReleaseId());
            validateTree(p, new HashSet<>(Set.of(a.code())), 1);
            children.add(p);
        }
        return new PublishedAgent("",0,a,model,List.copyOf(capabilities.values()),List.copyOf(children));
    }

    private void validateTree(PublishedAgent p, Set<String> path, int depth) {
        require(depth <= 5, ResponseMessage.Agent.AGENT_CHILD_DEPTH_EXCEEDED);
        require(p.agent().enabled(), ResponseMessage.Agent.AGENT_CHILD_RELEASE_DISABLED);
        require(path.add(p.agent().code()), ResponseMessage.Agent.AGENT_CHILD_CYCLE);
        SchemaSupport.checkSchema(p.agent().outputSchema());
        for (Capability capability : p.capabilities())
            if (capability.tool() != null)
                SchemaSupport.checkSchema(capability.tool().inputSchema());
        for (var c : p.children()) validateTree(c, new HashSet<>(path), depth + 1);
    }

    private void bind(Map<Long, Capability> out, List<Long> ids, CapabilityKind kind) {
        for (Long id : list(ids)) {
            Capability c = store.capability(id);
            require(
                    c.kind() == kind && c.enabled(),
                    ResponseMessage.Agent.CAPABILITY_BINDING_INVALID.formatted(c.code()));
            if (c.tool() != null) SchemaSupport.checkSchema(c.tool().inputSchema());
            out.put(id, c);
        }
    }

    private void generation(Generation g) {
        if (g == null) return;
        if (g.extra() != null) {
            Set<String> reserved =
                    Set.of(
                            "model",
                            "messages",
                            "tools",
                            "tool_choice",
                            "stream",
                            "api_key",
                            "base_url",
                            "response_format");
            require(
                    Collections.disjoint(g.extra().keySet(), reserved),
                    ResponseMessage.Agent.GENERATION_RESERVED_PARAMETER);
        }
    }

    public static void endpoint(String value) {
        try {
            URI u = URI.create(value);
            require(
                    Set.of("https", "http").contains(u.getScheme())
                            && u.getHost() != null
                            && u.getUserInfo() == null
                            && u.getFragment() == null,
                    ResponseMessage.Agent.ENDPOINT_INVALID);
        } catch (IceeBootException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IceeBootException(ResponseMessage.Agent.ENDPOINT_INVALID, e);
        }
    }

    public static void resourcePath(String path) {
        require(
                path != null
                        && !path.isBlank()
                        && !path.contains("\\")
                        && !path.contains("\u0000")
                        && !Path.of(path).isAbsolute()
                        && !Arrays.asList(path.split("/")).contains("..")
                        && !path.equals("SKILL.md"),
                ResponseMessage.Agent.SKILL_RESOURCE_PATH_INVALID);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IceeBootException(message);
    }

    private static List<Long> list(List<Long> value) {
        return value == null ? List.of() : value.stream().distinct().toList();
    }

    private Map<String, String> protect(Map<String, String> values, Map<String, String> old) {
        if (values == null) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        values.forEach(
                (k, v) -> {
                    require(
                            k != null && !k.isBlank() && !k.contains("\n") && !k.contains("\r"),
                            ResponseMessage.Agent.HEADER_OR_ENVIRONMENT_KEY_INVALID);
                    String secret = secrets.merge(v, old == null ? null : old.get(k));
                    if (secret != null) out.put(k, secret);
                });
        return out;
    }

    private Map<String, String> mask(Map<String, String> values) {
        if (values == null) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        values.forEach((k, v) -> out.put(k, secrets.mask(v)));
        return out;
    }

    private ModelConfig mask(ModelConfig m) {
        return new ModelConfig(
                m.id(),
                m.name(),
                m.provider(),
                m.protocol(),
                m.baseUrl(),
                secrets.mask(m.apiKey()),
                m.model(),
                m.contextWindow(),
                m.enabled(),
                m.generation());
    }

    private Capability mask(Capability c) {
        ToolConfig t = c.tool();
        McpConfig m = c.mcp();
        return new Capability(
                c.id(),
                c.kind(),
                c.code(),
                c.name(),
                c.description(),
                c.enabled(),
                c.skill(),
                t == null
                        ? null
                        : new ToolConfig(
                                t.kind(),
                                t.url(),
                                t.method(),
                                mask(t.headers()),
                                t.inputSchema(),
                                t.script(),
                                t.projectApi(),
                                t.permissionCode()),
                m == null ? null : new McpConfig(m.url(), mask(m.headers()), m.allowedTools()),
                c.sandbox());
    }

    private AgentView mask(AgentView v) {
        return new AgentView(
                environment(v.draft(), mask(v.draft().environment())),
                v.activeReleaseId(),
                v.activeVersion(),
                v.callCount());
    }

    private static AgentSpec environment(AgentSpec a, Map<String, String> env) {
        return new AgentSpec(
                a.id(),
                a.code(),
                a.name(),
                a.description(),
                a.avatar(),
                a.prompt(),
                a.modelConfigId(),
                a.maxRounds(),
                a.enabled(),
                a.sandboxEnabled(),
                a.sandboxTemplateId(),
                a.planMode(),
                a.outputFormat(),
                a.outputSchema(),
                a.generation(),
                a.skillIds(),
                a.toolIds(),
                a.mcpIds(),
                a.knowledgeIds(),
                a.childAgentIds(),
                env);
    }
}
