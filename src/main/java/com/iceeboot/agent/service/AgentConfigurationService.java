package com.iceeboot.agent.service;

import com.iceeboot.agent.model.AgentContracts.*;
import com.iceeboot.agent.persistence.AgentStore;
import com.iceeboot.agent.runtime.SchemaSupport;
import com.iceeboot.agent.security.AgentSecrets;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentConfigurationService {
  private final AgentStore store;
  private final AgentSecrets secrets;

  public AgentConfigurationService(AgentStore store, AgentSecrets secrets) {
    this.store = store;
    this.secrets = secrets;
  }

  public List<ModelConfig> models() {
    return store.models().stream().map(this::mask).toList();
  }

  public ModelConfig saveModel(ModelConfig m) {
    endpoint(m.baseUrl());
    if (m.provider() == Provider.CLAUDE && m.protocol() != Protocol.ANTHROPIC
        || m.provider() == Provider.GEMINI && m.protocol() != Protocol.GEMINI)
      throw new IllegalArgumentException("供应商与协议不匹配");
    if (m.provider() != Provider.CLAUDE
        && m.provider() != Provider.GEMINI
        && m.protocol() != Protocol.OPENAI_CHAT
        && m.protocol() != Protocol.OPENAI_RESPONSES)
      throw new IllegalArgumentException("该供应商应使用 OpenAI 兼容协议");
    if (m.provider() != Provider.GPT && m.protocol() == Protocol.OPENAI_RESPONSES)
      throw new IllegalArgumentException("当前仅为 GPT 开放 Responses 协议");
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
      throw new IllegalArgumentException("请填写 API Key");
    return mask(AgentStore.withId(saved, store.saveModel(saved)));
  }

  public List<Capability> capabilities() {
    return store.capabilities().stream().map(this::mask).toList();
  }

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
                      "load_skill_through_path",
                      "reset_tools",
                      "generate_response",
                      "wait_async_results",
                      "plan_enter",
                      "plan_write",
                      "plan_exit",
                      "todo_write")
                  .contains(c.code()),
          "工具编码与内置能力名称冲突");
    }
    Capability old = c.id() == null ? null : store.capability(c.id());
    if (old != null && (old.kind() != c.kind() || !old.code().equals(c.code())))
      throw new IllegalArgumentException("能力类型和编码创建后不能修改");
    SkillConfig skill = c.skill();
    ToolConfig tool = c.tool();
    McpConfig mcp = c.mcp();
    SandboxConfig sandbox = c.sandbox();
    switch (c.kind()) {
      case SKILL -> {
        require(
            skill != null && skill.content() != null && !skill.content().isBlank(), "请填写 Skill 内容");
        require(skill.content().length() <= 100000, "Skill 内容过长");
        if (skill.resources() != null) {
          require(skill.resources().size() <= 100, "每个 Skill 最多 100 个资源");
          require(
              skill.resources().values().stream()
                      .filter(Objects::nonNull)
                      .mapToLong(String::length)
                      .sum()
                  <= 5000000,
              "单个 Skill 的资源总量不能超过 5MB");
          for (var e : skill.resources().entrySet()) {
            resourcePath(e.getKey());
            require(e.getValue() != null && e.getValue().length() <= 1000000, "资源内容过长");
          }
        }
        for (Long id : list(skill.toolIds()))
          require(store.capability(id).kind() == CapabilityKind.TOOL, "Skill 只能关联 Tool");
        tool = null;
        mcp = null;
        sandbox = null;
      }
      case TOOL, KNOWLEDGE -> {
        require(tool != null && tool.kind() != null, "请配置工具执行方式");
        require(
            tool.inputSchema() != null && "object".equals(tool.inputSchema().get("type")),
            "工具参数必须是 object JSON Schema");
        SchemaSupport.checkSchema(tool.inputSchema());
        if (c.kind() == CapabilityKind.KNOWLEDGE)
          require(tool.kind() == ToolKind.HTTP, "知识库通过检索 HTTP 接口接入");
        if (tool.kind() == ToolKind.SCRIPT) {
          require(
              tool.script() != null && !tool.script().isBlank() && tool.script().length() <= 100000,
              "请填写脚本命令");
        } else if (tool.projectApi()) {
          require(
              tool.url() != null
                  && tool.url().startsWith("/")
                  && !tool.url().startsWith("//")
                  && !tool.url().contains("..")
                  && !tool.url().contains("\\"),
              "项目接口必须是以 / 开头的相对路径");
          require(tool.permissionCode() != null && !tool.permissionCode().isBlank(), "项目接口必须指定权限码");
        } else endpoint(tool.url());
        if (tool.kind() == ToolKind.HTTP)
          require(
              tool.method() != null
                  && Set.of("GET", "POST", "PUT", "PATCH", "DELETE").contains(tool.method()),
              "不支持的 HTTP 方法");
        tool =
            new ToolConfig(
                tool.kind(),
                tool.url(),
                tool.method(),
                protect(
                    tool.headers(),
                    old == null || old.tool() == null ? null : old.tool().headers()),
                tool.inputSchema(),
                tool.script(),
                tool.projectApi(),
                tool.permissionCode());
        skill = null;
        mcp = null;
        sandbox = null;
      }
      case MCP -> {
        require(mcp != null, "请配置 MCP");
        endpoint(mcp.url());
        require(
            mcp.allowedTools() != null
                && !mcp.allowedTools().isEmpty()
                && !mcp.allowedTools().contains("*"),
            "请明确填写允许调用的 MCP 工具名");
        mcp =
            new McpConfig(
                mcp.url(),
                protect(
                    mcp.headers(), old == null || old.mcp() == null ? null : old.mcp().headers()),
                mcp.allowedTools());
        skill = null;
        tool = null;
        sandbox = null;
      }
      case SANDBOX -> {
        require(
            sandbox != null && sandbox.templateId() != null && !sandbox.templateId().isBlank(),
            "请填写阿里云 E2B 模板 ID 或名称");
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
    return mask(AgentStore.withId(saved, store.saveCapability(saved)));
  }

  public List<AgentView> agents() {
    return store.agents().stream().map(this::mask).toList();
  }

  public AgentView agent(long id) {
    return mask(store.agent(id));
  }

  public AgentView saveAgent(AgentSpec a) {
    AgentSpec old = a.id() == null ? null : store.agent(a.id()).draft();
    if (old != null && !old.code().equals(a.code()))
      throw new IllegalArgumentException("Agent code 创建后不可修改");
    generation(a.generation());
    if (a.environment() != null)
      for (String key : a.environment().keySet())
        require(key.matches("[a-zA-Z_][a-zA-Z0-9_]*"), "环境变量名只能包含字母、数字和下划线，且不能以数字开头");
    if (a.outputFormat() == OutputFormat.JSON_SCHEMA)
      require(
          a.outputSchema() != null && "object".equals(a.outputSchema().get("type")),
          "请填写 object 类型的输出 JSON Schema");
    SchemaSupport.checkSchema(a.outputSchema());
    AgentSpec saved =
        environment(a, protect(a.environment(), old == null ? null : old.environment()));
    long id = store.saveAgent(saved);
    return agent(id);
  }

  @Transactional
  public ReleaseView publish(long id, String user) {
    store.lockAgent(id);
    AgentSpec a = store.agent(id).draft();
    ModelConfig model = store.model(a.modelConfigId());
    require(model.enabled(), "模型配置已禁用");
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
      require(a.sandboxTemplateId() != null, "请选择沙箱模板");
      bind(capabilities, List.of(a.sandboxTemplateId()), CapabilityKind.SANDBOX);
    }
    if (!a.sandboxEnabled())
      for (Capability c : capabilities.values())
        require(c.tool() == null || c.tool().kind() != ToolKind.SCRIPT, "脚本工具需要启用 E2B 沙箱");
    List<PublishedAgent> children = new ArrayList<>();
    for (Long child : list(a.childAgentIds())) {
      require(child != id, "不能关联自身");
      AgentView v = store.agent(child);
      require(v.draft().enabled(), "子智能体已禁用");
      PublishedAgent p = store.release(v.activeReleaseId());
      validateTree(p, new HashSet<>(Set.of(a.code())), 1);
      children.add(p);
    }
    String releaseId = UUID.randomUUID().toString();
    int version = store.nextVersion(id);
    PublishedAgent p =
        new PublishedAgent(
            releaseId,
            version,
            a,
            model,
            List.copyOf(capabilities.values()),
            List.copyOf(children));
    store.publish(p, user);
    return store.releases(id).stream()
        .filter(r -> r.id().equals(releaseId))
        .findFirst()
        .orElseThrow();
  }

  private void validateTree(PublishedAgent p, Set<String> path, int depth) {
    require(depth <= 5, "父子智能体层级最多 5 层");
    require(p.agent().enabled(), "子智能体发布版本已禁用");
    require(path.add(p.agent().code()), "父子智能体存在循环关联");
    for (var c : p.children()) validateTree(c, new HashSet<>(path), depth + 1);
  }

  private void bind(Map<Long, Capability> out, List<Long> ids, CapabilityKind kind) {
    for (Long id : list(ids)) {
      Capability c = store.capability(id);
      require(c.kind() == kind && c.enabled(), "绑定能力类型错误或已禁用：" + c.code());
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
      require(Collections.disjoint(g.extra().keySet(), reserved), "扩展参数不能覆盖模型、消息、工具或输出协议");
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
          "请输入有效 HTTP(S) 地址，不允许内嵌认证信息");
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("请输入有效 HTTP(S) 地址，不允许内嵌认证信息");
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
        "资源路径必须是安全的相对文件路径");
  }

  private static void require(boolean ok, String message) {
    if (!ok) throw new IllegalArgumentException(message);
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
              k != null && !k.isBlank() && !k.contains("\n") && !k.contains("\r"), "环境变量或请求头名称不合法");
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
