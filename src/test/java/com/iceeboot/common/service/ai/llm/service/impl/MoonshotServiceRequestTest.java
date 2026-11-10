package com.iceeboot.common.service.ai.llm.service.impl;

import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoonshotServiceRequestTest {

    @Test
    void shouldAcceptBaseOrCompleteChatEndpoint() {
        MoonshotService service = new MoonshotService();

        assertThat((String) ReflectionTestUtils.invokeMethod(service, "buildChatEndpoint",
                "https://api.moonshot.cn/v1"))
                .isEqualTo("https://api.moonshot.cn/v1/chat/completions");
        assertThat((String) ReflectionTestUtils.invokeMethod(service, "buildChatEndpoint",
                "https://api.moonshot.cn/v1/chat/completions/"))
                .isEqualTo("https://api.moonshot.cn/v1/chat/completions");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExplicitlyDisableThinkingForKimiK26() {
        MoonshotService service = new MoonshotService();
        LLMRequest request = new LLMRequest()
                .setModel("kimi-k2.6")
                .setUserMessage("test")
                .setThinking(false);

        Mono<Map<String, Object>> result = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);

        assertThat(result.block())
                .containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldEnableThinkingForKimiK26OnlyWhenRequested() {
        MoonshotService service = new MoonshotService();
        LLMRequest request = new LLMRequest()
                .setModel("kimi-k2.6")
                .setUserMessage("test")
                .setThinking(true);

        Mono<Map<String, Object>> result = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);

        assertThat(result.block())
                .containsEntry("thinking", Map.of("type", "enabled"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapExtremeKimiK3ThinkingToVendorMax() {
        MoonshotService service = new MoonshotService();
        LLMRequest request = new LLMRequest()
                .setModel("kimi-k3")
                .setUserMessage("test")
                .setThinking(true)
                .setReasoningEffort("xhigh");

        Mono<Map<String, Object>> result = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);

        assertThat(result.block()).containsEntry("reasoning_effort", "max");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotSendThinkingParameterToLegacyKimiModels() {
        MoonshotService service = new MoonshotService();
        LLMRequest request = new LLMRequest()
                .setModel("moonshot-v1-8k")
                .setUserMessage("test")
                .setThinking(false);

        Mono<Map<String, Object>> result = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);

        assertThat(result.block()).doesNotContainKey("thinking");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectAlwaysThinkingKimiModelInNormalMode() {
        MoonshotService service = new MoonshotService();
        LLMRequest request = new LLMRequest()
                .setModel("kimi-k3")
                .setUserMessage("test")
                .setThinking(false);

        Mono<Map<String, Object>> result = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);

        assertThatThrownBy(result::block)
                .hasMessageContaining("固定开启思考");
    }

    @Test
    void shouldExtractSsePayloadAndReasoningContent() {
        MoonshotService service = new MoonshotService();

        assertThat(service.extractStreamPayloads("event: message\ndata: {\"choices\":[{\"delta\":{\"reasoning_content\":\"分析\"}}]}\n\ndata: [DONE]\n"))
                .singleElement().asString().contains("reasoning_content");
        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parseStreamResponse",
                "{\"choices\":[{\"delta\":{\"reasoning_content\":\"分析\"},\"finish_reason\":null}]}");
        assertThat(response.getExtraData())
                .containsEntry("reasoning_content_delta", "分析")
                .containsEntry("reasoning_type", "content");
    }
}
