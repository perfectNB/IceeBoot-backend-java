package com.iceeboot.agent.runtime;

import com.iceeboot.agent.config.AgentProperties;
import com.iceeboot.agent.model.AgentContracts.*;
import com.iceeboot.agent.persistence.AgentStore;
import com.iceeboot.agent.security.AgentSecrets;
import com.iceeboot.framework.service.system.ISysUserService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import io.agentscope.core.permission.*;
import io.agentscope.core.tool.*;
import io.agentscope.core.tool.mcp.*;
import io.agentscope.extensions.sandbox.e2b.*;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** 参考 Swarm 的配置装配边界，业务数据不进入框架包，也不覆盖 SDK 内核类。 */
@Component
public class HarnessRuntimeFactory {
  private final AgentModelFactory models;
  private final AgentProperties properties;
  private final AgentSecrets secrets;
  private final AgentStore store;
  private final ISysUserService users;

  public HarnessRuntimeFactory(
      AgentModelFactory models,
      AgentProperties properties,
      AgentSecrets secrets,
      AgentStore store,
      ISysUserService users) {
    this.models = models;
    this.properties = properties;
    this.secrets = secrets;
    this.store = store;
    this.users = users;
  }

  public HarnessAgent create(
      PublishedAgent p,
      Caller caller,
      RunRecorder recorder,
      AtomicInteger budget,
      List<AutoCloseable> resources,
      String session) {
    Toolkit toolkit = new Toolkit();
    // 显式工具与已绑定 Skill 的关联工具均来自同一个不可变能力白名单。
    for (var c : p.capabilities()) {
      if (c.kind() == CapabilityKind.TOOL || c.kind() == CapabilityKind.KNOWLEDGE)
        toolkit.registerAgentTool(
            new ConfiguredTool(
                c, caller, properties, secrets, store, users, p.agent().sandboxEnabled()));
    }
    for (var c : p.capabilities())
      if (c.kind() == CapabilityKind.MCP) {
        var config = c.mcp();
        var builder =
            McpClientBuilder.create(c.code())
                .streamableHttpTransport(config.url())
                .timeout(Duration.ofSeconds(30))
                .initializationTimeout(Duration.ofSeconds(15));
        if (config.headers() != null)
          config.headers().forEach((k, v) -> builder.header(k, secrets.decrypt(v)));
        McpClientWrapper client = builder.buildAsync().block(Duration.ofSeconds(20));
        if (client == null) throw new IllegalStateException("MCP 连接失败");
        resources.add(client);
        Set<String> names = new HashSet<>();
        client.listTools().block(Duration.ofSeconds(15)).forEach(t -> names.add(t.name()));
        if (!names.containsAll(config.allowedTools()))
          throw new IllegalArgumentException("MCP 配置包含不存在的工具：" + c.code());
        toolkit
            .registration()
            .mcpClient(client)
            .mcpToolNamePrefix(c.code() + "__")
            .enableTools(config.allowedTools())
            .apply();
      }
    for (var child : p.children()) {
      // 每次成员调用创建独立 Harness 与账本，避免工具注册时提前构建或复用已结束的调用身份。
      toolkit.registerAgentTool(
          new BoundSubagentTool(
              child.agent().code(),
              child.agent().description() == null
                  ? child.agent().name()
                  : child.agent().description(),
              task ->
                  Mono.defer(
                      () -> {
                        RunRecorder childRun =
                            new RunRecorder(
                                recorder.rootId, recorder.id, child, caller, store, budget);
                        var input = List.<Msg>of(new UserMessage(task));
                        childRun.start(input);
                        return Mono.defer(
                                () -> {
                                  HarnessAgent childAgent =
                                      create(
                                          child, caller, childRun, budget, resources, childRun.id);
                                  resources.add(childAgent);
                                  RuntimeContext ctx =
                                      RuntimeContext.builder()
                                          .userId(caller.userId())
                                          .sessionId(childRun.id)
                                          .runId(childRun.id)
                                          .build();
                                  Mono<Msg> call =
                                      child.agent().outputFormat() == OutputFormat.JSON_SCHEMA
                                          ? childAgent.call(
                                              input,
                                              new com.fasterxml.jackson.databind.ObjectMapper()
                                                  .valueToTree(child.agent().outputSchema()),
                                              ctx)
                                          : childAgent.call(input, ctx);
                                  return call.doFinally(
                                      signal -> {
                                        childAgent.close();
                                        resources.remove(childAgent);
                                      });
                                })
                            .doOnError(e -> childRun.fail());
                      })));
    }
    var permissions = PermissionContextState.builder().mode(PermissionMode.DONT_ASK);
    Set<String> allowed = new LinkedHashSet<>(toolkit.getToolNames());
    allowed.addAll(
        List.of(
            "load_skill_through_path", "reset_tools", "reset_equipped_tools", "generate_response"));
    if (p.agent().sandboxEnabled())
      allowed.addAll(
          List.of(
              "execute", "read_file", "write_file", "edit_file", "list_directory", "glob", "grep"));
    if (p.agent().planMode())
      allowed.addAll(List.of("plan_enter", "plan_write", "plan_exit", "todo_write"));
    for (String name : allowed)
      permissions.addAllowRule(
          name, new PermissionRule(name, "", PermissionBehavior.ALLOW, "published_binding"));
    String prompt = p.agent().prompt();
    if (p.agent().outputFormat() == OutputFormat.JSON_SCHEMA)
      prompt += "\n最终结果必须是符合以下 JSON Schema 的纯 JSON：\n" + store.json(p.agent().outputSchema());
    Path workspacePath =
        Path.of(properties.getWorkspace(), p.releaseId(), caller.userId(), session);
    try {
      Files.createDirectories(workspacePath);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("无法创建运行工作区", e);
    }
    var builder =
        HarnessAgent.builder()
            .name(p.agent().code())
            .agentId(p.releaseId())
            .sysPrompt(prompt)
            .model(recorder.observe(models.create(p.model(), p.agent().generation())))
            .generateOptions(models.options(p.model(), p.agent().generation()))
            .modelExecutionConfig(
                ExecutionConfig.builder().maxAttempts(1).timeout(Duration.ofSeconds(120)).build())
            .toolkit(toolkit)
            .permissionContext(permissions.build())
            .maxIters(p.agent().maxRounds())
            .workspace(workspacePath)
            .disableDefaultWorkspaceSkills()
            .disableDynamicSkills()
            .disableDynamicSubagents()
            .disableSubagents()
            .disableToolsConfig()
            .disableWorkspaceContext()
            .disableAtPathExpansion()
            .disableMemoryHooks()
            .disableMemoryTools()
            .disableWebTools()
            .disableToolResultEviction()
            .enableAgentTracingLog(false)
            .middleware(recorder);
    List<Capability> skills =
        p.capabilities().stream().filter(c -> c.kind() == CapabilityKind.SKILL).toList();
    if (!skills.isEmpty()) builder.skillRepository(new SnapshotSkillRepository(skills));
    if (p.agent().planMode()) builder.enablePlanMode().enableTaskList();
    if (p.agent().sandboxEnabled()) {
      var e2b = properties.getE2b();
      if (e2b.getApiKey().isBlank() || e2b.getApiBaseUrl().isBlank() || e2b.getDomain().isBlank())
        throw new IllegalStateException("请在 application 中配置阿里云 E2B 连接信息");
      Capability template =
          p.capabilities().stream()
              .filter(
                  c ->
                      c.kind() == CapabilityKind.SANDBOX
                          && Objects.equals(c.id(), p.agent().sandboxTemplateId()))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("发布版本没有沙箱模板"));
      WorkspaceSpec workspace = new WorkspaceSpec();
      workspace.setRoot("/home/user/workspace");
      Map<String, String> env = new LinkedHashMap<>();
      if (p.agent().environment() != null)
        p.agent().environment().forEach((k, v) -> env.put(k, secrets.decrypt(v)));
      workspace.setEnvironment(env);
      E2bFilesystemSpec spec =
          new E2bFilesystemSpec()
              .apiKey(e2b.getApiKey())
              .apiBaseUrl(e2b.getApiBaseUrl())
              .domain(e2b.getDomain())
              .templateId(template.sandbox().templateId())
              .workspaceRoot(workspace.getRoot())
              .workspaceSpec(workspace)
              .codec(E2bCodec.JSON)
              .sandboxTimeoutSeconds(
                  template.sandbox().timeoutSeconds() == null
                      ? 600
                      : template.sandbox().timeoutSeconds());
      spec.isolationScope(IsolationScope.SESSION);
      builder.filesystem(spec);
    } else builder.disableFilesystemTools().disableShellTool();
    return builder.build();
  }
}
