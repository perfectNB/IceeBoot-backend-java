package com.iceeboot.agent;

import static org.assertj.core.api.Assertions.*;

import com.iceeboot.agent.model.AgentContracts.*;
import com.iceeboot.agent.runtime.AgentModelFactory;
import com.sun.net.httpserver.HttpServer;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.ToolSchema;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AgentModelFactoryTest {
  @ParameterizedTest
  @EnumSource(Provider.class)
  void createsEveryRequestedProviderWithoutProviderSpecificHttpStrategies(Provider provider)
      throws Exception {
    AgentFixture f = new AgentFixture();
    var protocol =
        provider == Provider.CLAUDE
            ? Protocol.ANTHROPIC
            : provider == Provider.GEMINI ? Protocol.GEMINI : Protocol.OPENAI_CHAT;
    var c =
        new ModelConfig(
            1L,
            "测试",
            provider,
            protocol,
            "https://example.invalid/v1",
            f.secrets.encrypt("fixture-key"),
            "fixture-model",
            32768,
            true,
            new Generation(null, 8192, true, "high", 2048, Map.of()));
    var model = new AgentModelFactory(f.secrets).create(c, null);
    assertThat(model.getModelName()).isEqualTo("fixture-model");
  }

  @Test
  void responsesProtocolCreatesDedicatedClient() throws Exception {
    AgentFixture f = new AgentFixture();
    var c =
        new ModelConfig(
            1L,
            "测试",
            Provider.GPT,
            Protocol.OPENAI_RESPONSES,
            "https://example.invalid/v1",
            f.secrets.encrypt("fixture-key"),
            "fixture-model",
            32768,
            true,
            null);
    assertThat(new AgentModelFactory(f.secrets).create(c, null).getClass().getSimpleName())
        .contains("Responses");
  }

  @Test
  void sdkCarriesToolsAndReasoningOptionsThroughCustomBaseUrl() throws Exception {
    AtomicReference<String> body = new AtomicReference<>();
    AtomicReference<String> auth = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v1/chat/completions",
        e -> {
          body.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          auth.set(e.getRequestHeaders().getFirst("Authorization"));
          String s =
              "data:"
                  + " {\"id\":\"r\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"ok\"},\"finish_reason\":null}]}\n\n"
                  + "data:"
                  + " {\"id\":\"r\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":11,\"completion_tokens\":2,\"total_tokens\":13}}\n\n"
                  + "data: [DONE]\n\n";
          byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
          e.getResponseHeaders().set("Content-Type", "text/event-stream");
          e.sendResponseHeaders(200, bytes.length);
          e.getResponseBody().write(bytes);
          e.close();
        });
    server.start();
    try {
      AgentFixture f = new AgentFixture();
      var config =
          new ModelConfig(
              1L,
              "测试",
              Provider.DEEPSEEK,
              Protocol.OPENAI_CHAT,
              "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
              f.secrets.encrypt("fixture-key"),
              "fixture-model",
              32768,
              true,
              new Generation(null, 8192, true, null, null, Map.of()));
      var factory = new AgentModelFactory(f.secrets);
      var response =
          factory.create(config, null).stream(
                  List.of(new UserMessage("test")),
                  List.of(
                      ToolSchema.builder()
                          .name("lookup")
                          .description("查询")
                          .parameters(Map.of("type", "object", "properties", Map.of()))
                          .build()),
                  factory.options(config, null))
              .collectList()
              .block();
      assertThat(response).isNotEmpty();
      assertThat(auth.get()).isEqualTo("Bearer fixture-key");
      assertThat(body.get()).contains("lookup", "fixture-model", "thinking");
      assertThat(response.stream().filter(r -> r.getUsage() != null).toList()).isNotEmpty();
    } finally {
      server.stop(0);
    }
  }
}
