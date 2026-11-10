package com.iceeboot.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.agent.model.AgentContracts.*;
import com.iceeboot.agent.runtime.*;
import com.iceeboot.agent.service.AgentExecutionService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.sun.net.httpserver.HttpServer;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;

class HarnessExecutionTest {
  AgentFixture f;
  AgentModelFactory models;
  ISysUserService users;
  AgentExecutionService service;

  @BeforeEach
  void setup() throws Exception {
    f = new AgentFixture();
    models = mock(AgentModelFactory.class);
    users = mock(ISysUserService.class);
    when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
    var factory = new HarnessRuntimeFactory(models, f.properties, f.secrets, f.store, users);
    service = new AgentExecutionService(f.store, factory, models, f.properties, f.json);
  }

  static Model model(BiFunction<List<Msg>, List<ToolSchema>, ChatResponse> handler) {
    return new Model() {
      public String getModelName() {
        return "test-model";
      }

      public int getContextWindowSize() {
        return 16384;
      }

      public Flux<ChatResponse> stream(
          List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        return Flux.defer(() -> Flux.just(handler.apply(messages, tools)));
      }
    };
  }

  static ChatResponse text(String value, int in, int out) {
    return ChatResponse.builder()
        .id(UUID.randomUUID().toString())
        .content(List.of(TextBlock.builder().text(value).build()))
        .usage(new ChatUsage(in, out, 0.01))
        .finishReason("stop")
        .build();
  }

  static ChatResponse tool(String name, Map<String, Object> input) {
    return ChatResponse.builder()
        .id(UUID.randomUUID().toString())
        .content(
            List.of(
                ToolUseBlock.builder()
                    .id("call_1")
                    .name(name)
                    .input(input)
                    .content(
                        new com.fasterxml.jackson.databind.ObjectMapper()
                            .valueToTree(input)
                            .toString())
                    .build()))
        .usage(new ChatUsage(10, 3, 0.01))
        .finishReason("tool_calls")
        .build();
  }

  @Test
  void harnessExecutesBoundHttpToolAndRecordsEachModelRequest() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/data",
        e -> {
          requests.incrementAndGet();
          byte[] body = "{\"value\":42}".getBytes();
          e.sendResponseHeaders(200, body.length);
          e.getResponseBody().write(body);
          e.close();
        });
    server.start();
    try {
      var m = f.model();
      var t = f.http("lookup", "http://127.0.0.1:" + server.getAddress().getPort() + "/data");
      f.publish(
          f.save("analyst", m.id(), List.of(), List.of(t.id()), List.of(), List.of(), false, null));
      AtomicInteger calls = new AtomicInteger();
      when(models.create(any(), any()))
          .thenReturn(
              model(
                  (messages, tools) -> {
                    assertThat(tools)
                        .extracting(ToolSchema::getName)
                        .doesNotContain("execute", "read_file");
                    if (calls.getAndIncrement() == 0) return tool("lookup", Map.of());
                    assertThat(f.store.json(messages)).contains("42");
                    return text("结果是42", 20, 5);
                  }));
      var result =
          service.invoke("analyst", new InvokeRequest("查询数据", null), new Caller("1", "token"));
      assertThat(result.output()).isEqualTo("结果是42");
      assertThat(result.inputTokens()).isEqualTo(30);
      assertThat(result.outputTokens()).isEqualTo(8);
      assertThat(requests).hasValue(1);
      var detail = f.store.runDetail(result.runId());
      assertThat(detail.modelCalls()).hasSize(2);
      assertThat(detail.agents().getFirst().status()).isEqualTo("SUCCEEDED");
    } finally {
      server.stop(0);
    }
  }

  @Test
  void unknownToolsCannotExecute() {
    var m = f.model();
    f.publish(f.save("no_tools", m.id(), List.of(), List.of(), List.of(), List.of(), false, null));
    AtomicInteger calls = new AtomicInteger();
    when(models.create(any(), any()))
        .thenReturn(
            model(
                (messages, tools) ->
                    calls.getAndIncrement() == 0
                        ? tool("execute", Map.of())
                        : text("没有该工具", 3, 1)));
    var r = service.invoke("no_tools", new InvokeRequest("尝试工具", null), new Caller("1", "token"));
    assertThat(r.output()).isEqualTo("没有该工具");
  }

  @Test
  void childAgentUsesOwnHarnessAndTokensAreCountedOnce() {
    var m = f.model();
    var child = f.save("child", m.id(), List.of(), List.of(), List.of(), List.of(), false, null);
    f.publish(child);
    f.publish(
        f.save(
            "parent",
            m.id(),
            List.of(),
            List.of(),
            List.of(),
            List.of(child.draft().id()),
            false,
            null));
    AtomicInteger calls = new AtomicInteger();
    Model parentModel =
        model(
            (messages, tools) -> {
              if (calls.getAndIncrement() == 0) {
                var target =
                    tools.stream()
                        .filter(t -> t.getName().equals("agent_child"))
                        .findFirst()
                        .orElseThrow();
                return tool(target.getName(), Map.of("task", "计算一下"));
              }
              assertThat(f.store.json(messages)).contains("子智能体完成");
              return text("父智能体完成", 20, 5);
            });
    when(models.create(any(), any()))
        .thenReturn(parentModel, model((messages, tools) -> text("子智能体完成", 7, 2)));
    var result = service.invoke("parent", new InvokeRequest("开始", null), new Caller("1", "token"));
    var detail = f.store.runDetail(result.runId());
    assertThat(detail.agents()).hasSize(2);
    assertThat(detail.modelCalls()).hasSize(3);
    assertThat(result.inputTokens()).isEqualTo(37);
    assertThat(result.outputTokens()).isEqualTo(10);
    assertThat(detail.agents())
        .filteredOn(r -> r.agentCode().equals("child"))
        .allMatch(r -> r.parentRunId().equals(result.runId()));
  }

  @Test
  void usageIsLastStreamSnapshotNotSum() {
    var p =
        f.publish(
            f.save(
                "count", f.model().id(), List.of(), List.of(), List.of(), List.of(), false, null));
    RunRecorder recorder =
        new RunRecorder(null, "", p, new Caller("1", "token"), f.store, new AtomicInteger(10));
    recorder.start(List.of(new UserMessage("input")));
    Model delegate =
        new Model() {
          public String getModelName() {
            return "m";
          }

          public Flux<ChatResponse> stream(List<Msg> a, List<ToolSchema> b, GenerateOptions c) {
            return Flux.just(text("a", 5, 1), text("b", 5, 2));
          }
        };
    recorder.observe(delegate).stream(List.of(), List.of(), null).blockLast();
    var detail = f.store.runDetail(recorder.id);
    assertThat(detail.totalInputTokens()).isEqualTo(5);
    assertThat(detail.totalOutputTokens()).isEqualTo(2);
  }

  @Test
  void missingE2bCannotFallBackToLocalShell() {
    var m = f.model();
    var template =
        f.configs.saveCapability(
            new Capability(
                null,
                CapabilityKind.SANDBOX,
                "e2b",
                "E2B",
                "测试",
                true,
                null,
                null,
                null,
                new SandboxConfig("test-template", 600)));
    f.publish(
        f.save("sandbox", m.id(), List.of(), List.of(), List.of(), List.of(), true, template.id()));
    when(models.create(any(), any())).thenReturn(model((messages, tools) -> text("不应执行", 1, 1)));
    assertThatThrownBy(
            () ->
                service.invoke(
                    "sandbox", new InvokeRequest("curl", null), new Caller("1", "token")))
        .hasMessageContaining("E2B");
    assertThat(f.store.runs().getFirst().status()).isEqualTo("FAILED");
  }

  @Test
  void failedChildConstructionIsRecordedAsFailedNotLeftRunning() {
    var m = f.model();
    var template =
        f.configs.saveCapability(
            new Capability(
                null,
                CapabilityKind.SANDBOX,
                "child_e2b",
                "E2B",
                "测试",
                true,
                null,
                null,
                null,
                new SandboxConfig("test-template", 600)));
    var child =
        f.save(
            "child_fail", m.id(), List.of(), List.of(), List.of(), List.of(), true, template.id());
    f.publish(child);
    f.publish(
        f.save(
            "parent_fail",
            m.id(),
            List.of(),
            List.of(),
            List.of(),
            List.of(child.draft().id()),
            false,
            null));
    AtomicInteger calls = new AtomicInteger();
    when(models.create(any(), any()))
        .thenReturn(
            model(
                (messages, tools) ->
                    calls.getAndIncrement() == 0
                        ? tool("agent_child_fail", Map.of("task", "执行"))
                        : text("子调用未完成", 5, 2)));
    var result =
        service.invoke("parent_fail", new InvokeRequest("开始", null), new Caller("1", "token"));
    assertThat(f.store.runDetail(result.runId()).agents())
        .filteredOn(r -> r.agentCode().equals("child_fail"))
        .singleElement()
        .extracting(RunView::status)
        .isEqualTo("FAILED");
  }

  @Test
  void sameSessionCannotRunConcurrentlyAndRejectedCallDoesNotReleaseAnotherLock() throws Exception {
    var m = f.model();
    f.publish(
        f.save("session_agent", m.id(), List.of(), List.of(), List.of(), List.of(), false, null));
    var response = reactor.core.publisher.Sinks.<ChatResponse>one();
    var entered = new java.util.concurrent.CountDownLatch(1);
    when(models.create(any(), any()))
        .thenReturn(
            new Model() {
              public String getModelName() {
                return "test-model";
              }

              public int getContextWindowSize() {
                return 16384;
              }

              public Flux<ChatResponse> stream(List<Msg> a, List<ToolSchema> b, GenerateOptions c) {
                return response.asMono().doOnSubscribe(s -> entered.countDown()).flux();
              }
            });
    var first =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () ->
                service.invoke(
                    "session_agent",
                    new InvokeRequest("开始", "same_session"),
                    new Caller("1", "token")));
    assertThat(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    try {
      for (int i = 0; i < 2; i++)
        assertThatThrownBy(
                () ->
                    service.invoke(
                        "session_agent",
                        new InvokeRequest("开始", "same_session"),
                        new Caller("1", "token")))
            .hasMessageContaining("当前会话正在执行");
    } finally {
      response.tryEmitValue(text("完成", 5, 1));
    }
    assertThat(first.get(5, java.util.concurrent.TimeUnit.SECONDS).output()).isEqualTo("完成");
  }
}
