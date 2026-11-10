package com.iceeboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.service.ai.llm.dto.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercises real HTTP serialization, protocol fallback, and SSE decoding with a synthetic relay. */
class ChatGPTRelayHttpTest {
    @Test void fallsBackFromUnsupportedResponsesToVisionChatWithCorrectThinkingParameters() throws Exception {
        var requests = new CopyOnWriteArrayList<Map<String,Object>>();
        var paths = new CopyOnWriteArrayList<String>();
        var json = new ObjectMapper();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            requests.add(json.readValue(exchange.getRequestBody(), Map.class));
            String body = exchange.getRequestURI().getPath().endsWith("responses") ? "{}" : "{\"model\":\"gpt-6.1-terra\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"5274\"}}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":4,\"completion_tokens_details\":{\"reasoning_tokens\":0}}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().endsWith("responses") ? 501 : 200, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var service = serviceForFixture();
            var request = input(server).setThinking(false).setMaxTokens(2048).setMessages(List.of(LLMMessage.user(List.of(LLMContentPart.text("read"),LLMContentPart.imageUrl("data:image/png;base64,abc","original")))));
            var result = service.chat(request).block(Duration.ofSeconds(5));
            assertThat(result.getError()).isNull();
            assertThat(result.getChoices().get(0).getMessage().getContent()).isEqualTo("5274");
            assertThat(result.getExtraData()).containsEntry("reasoning_tokens",0L);
            assertThat(paths).containsExactly("/v1/responses","/v1/chat/completions");
            assertThat(requests.get(1)).containsEntry("reasoning_effort","none").containsEntry("max_completion_tokens",2048).doesNotContainKey("max_tokens");
            assertThat(json.writeValueAsString(requests.get(1))).contains("\"detail\":\"high\"");
        } finally { server.stop(0); }
    }
    @Test void streamsThinkingAndAnswerWithoutRetryingAnAuthenticationFailure() throws Exception {
        var calls = new CopyOnWriteArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/", exchange -> {
            calls.add(exchange.getRequestURI().getPath()); exchange.getRequestBody().readAllBytes();
            String body = exchange.getRequestURI().getPath().endsWith("responses") ? "{\"error\":{\"message\":\"synthetic-key rejected\"}}" : "data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"checking\"}}]}\n\ndata: {\"choices\":[{\"delta\":{\"content\":\"42\"}}]}\n\ndata: [DONE]\n\n";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", exchange.getRequestURI().getPath().endsWith("responses") ? "application/json" : "text/event-stream");
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().endsWith("responses") ? 401 : 200,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var service = serviceForFixture();
            var denied = service.chat(input(server)).block(Duration.ofSeconds(5));
            assertThat(denied.getError()).contains("401").doesNotContain("synthetic-key");
            assertThat(calls).containsExactly("/v1/responses");
            var stream = service.chatStream(input(server).setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/chat/completions").setThinking(true).setReasoningEffort("low")).collectList().block(Duration.ofSeconds(5));
            assertThat(stream).hasSize(2);
            assertThat(stream.get(0).getExtraData()).containsEntry("reasoning_content_delta","checking");
            assertThat(stream.get(1).getChoices().get(0).getDelta().getContent()).isEqualTo("42");
        } finally { server.stop(0); }
    }
    private ChatGPTService serviceForFixture() {
        var service = new ChatGPTService();
        // Only the fixture replaces DNS protection: product requests still use the guarded client.
        ReflectionTestUtils.setField(service,"publicEndpointWebClient",WebClient.create());
        return service;
    }
    private LLMRequest input(HttpServer server) {
        return new LLMRequest().setModel("gpt-6.1-terra").setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort()).setApiKey("synthetic-key").setPublicEndpointOnly(true).setTimeout(3).setUserMessage("test");
    }
}
