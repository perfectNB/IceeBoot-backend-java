package com.iceeboot.common.service.ai.llm.service.impl;

import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMContentPart;
import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatGPTServiceTest {

    @Test
    void shouldExposeGpt56AsSupportedModel() {
        ChatGPTService service = new ChatGPTService();

        assertThat(service.getSupportedModels(null, null).block())
                .isNotNull()
                .startsWith("gpt-5.6-sol")
                .contains("gpt-5.6-terra", "gpt-5.6-luna", "gpt-5.4");
        assertThat(service.getDefaultModel()).isEqualTo("gpt-5.6-sol");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildNonStreamingHighReasoningResponsesRequest() {
        ChatGPTService service = new ChatGPTService();
        LLMRequest request = new LLMRequest()
                .setModel("gpt-5.6-terra")
                .setSystemPrompt("系统改写指令")
                .setUserMessage("示例正文")
                .setMaxTokens(4096)
                .setReasoningEffort("high");

        Mono<Map<String, Object>> bodyMono = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, false);
        Map<String, Object> body = bodyMono.block();

        assertThat(body)
                .containsEntry("model", "gpt-5.6-terra")
                .containsEntry("stream", false)
                .containsEntry("instructions", "系统改写指令")
                .containsEntry("max_output_tokens", 4096)
                .containsEntry("reasoning", Map.of("effort", "high"))
                .doesNotContainKey("temperature");
        List<Map<String, Object>> input = (List<Map<String, Object>>) body.get("input");
        assertThat(input).singleElement().satisfies(message -> {
            assertThat(message).containsEntry("role", "user");
            List<Map<String, String>> content = (List<Map<String, String>>) message.get("content");
            assertThat(content).singleElement().satisfies(part -> assertThat(part)
                    .containsEntry("type", "input_text")
                    .containsEntry("text", "示例正文"));
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildResponsesMultimodalInput() {
        ChatGPTService service = new ChatGPTService();
        LLMRequest request = new LLMRequest()
                .setMessages(List.of(
                        LLMMessage.system("截图识别指令"),
                        LLMMessage.user(List.of(
                                LLMContentPart.text("请读取这张截图"),
                                LLMContentPart.imageUrl("data:image/png;base64,abc", "original")))));

        Mono<Map<String, Object>> bodyMono = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);
        Map<String, Object> body = bodyMono.block();

        List<Map<String, Object>> input = (List<Map<String, Object>>) body.get("input");
        assertThat(input).hasSize(2);
        List<Map<String, Object>> content = (List<Map<String, Object>>) input.get(1).get("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0))
                .containsEntry("type", "input_text")
                .containsEntry("text", "请读取这张截图");
        assertThat(content.get(1))
                .containsEntry("type", "input_image")
                .containsEntry("image_url", "data:image/png;base64,abc")
                .containsEntry("detail", "original");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDisableReasoningWhenRequestDisablesReasoning() {
        ChatGPTService service = new ChatGPTService();
        LLMRequest request = new LLMRequest()
                .setModel("gpt-5.6-sol")
                .setUserMessage("读取截图")
                .setThinking(false)
                .setReasoningEffort("none");

        Mono<Map<String, Object>> bodyMono = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);
        Map<String, Object> body = bodyMono.block();

        assertThat(body).containsEntry("reasoning", Map.of("effort", "none"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTranslateNormalModeToNoReasoningForSupportedGpt() {
        ChatGPTService service = new ChatGPTService();
        LLMRequest request = new LLMRequest()
                .setModel("gpt-5.6-sol")
                .setUserMessage("普通请求")
                .setThinking(false);

        Mono<Map<String, Object>> bodyMono = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);

        assertThat(bodyMono.block()).containsEntry("reasoning", Map.of("effort", "none"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapExtremeReasoningByGptModelCapability() {
        ChatGPTService service = new ChatGPTService();
        Mono<Map<String, Object>> newest = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("gpt-5.6-sol").setUserMessage("做题")
                        .setThinking(true).setReasoningEffort("xhigh"), true);
        Mono<Map<String, Object>> older = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("gpt-5.1").setUserMessage("做题")
                        .setThinking(true).setReasoningEffort("xhigh"), true);

        assertThat(newest.block()).containsEntry("reasoning", Map.of("effort", "xhigh", "summary", "auto"));
        assertThat(older.block()).containsEntry("reasoning", Map.of("effort", "high", "summary", "auto"));
    }

    @Test
    void shouldUseReasoningForGpt6ButKeepGpt4oAsOrdinaryScreenshotSolving() {
        ChatGPTService service = new ChatGPTService();
        Mono<Map<String, Object>> gpt6 = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("gpt-6-sol").setUserMessage("读取截图")
                        .setThinking(true).setReasoningEffort("xhigh"), true);
        Mono<Map<String, Object>> gpt4o = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("gpt-4o").setUserMessage("读取截图")
                        .setThinking(true).setReasoningEffort("xhigh"), true);

        assertThat(gpt6.block()).containsEntry("reasoning", Map.of("effort", "xhigh", "summary", "auto"));
        assertThat(gpt4o.block()).doesNotContainKey("reasoning");
    }

    @Test
    void shouldRejectNormalModeForGpt6ModelWithoutNoneEffort() {
        ChatGPTService service = new ChatGPTService();
        Mono<Map<String, Object>> astra = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("gpt-6-astra").setUserMessage("普通请求")
                        .setThinking(false).setReasoningEffort("none"), true);
        Mono<Map<String, Object>> sol = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("gpt-6-sol").setUserMessage("普通请求")
                        .setThinking(false).setReasoningEffort("none"), true);

        assertThatThrownBy(astra::block).hasMessageContaining("不支持关闭思考");
        assertThat(sol.block()).containsEntry("reasoning", Map.of("effort", "none"));
    }

    @Test
    void shouldKeepGpt5ProAtItsOnlySupportedHighEffort() {
        ChatGPTService service = new ChatGPTService();
        Mono<Map<String, Object>> body = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("gpt-5-pro").setUserMessage("做题")
                        .setThinking(true).setReasoningEffort("low"), true);

        assertThat(body.block()).containsEntry("reasoning", Map.of("effort", "high", "summary", "auto"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectReasoningOnlyGptThatCannotBeDisabled() {
        ChatGPTService service = new ChatGPTService();
        LLMRequest request = new LLMRequest()
                .setModel("o3")
                .setUserMessage("普通请求")
                .setThinking(false);

        Mono<Map<String, Object>> bodyMono = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, true);

        assertThatThrownBy(bodyMono::block)
                .hasMessageContaining("不支持关闭思考");
    }

    @Test
    void shouldExtractResponsesSsePayloads() {
        ChatGPTService service = new ChatGPTService();

        assertThat(service.extractStreamPayloads(
                "event: response.output_text.delta\ndata: {\"type\":\"response.output_text.delta\",\"delta\":\"答案\"}\n\n"
                        + "data: [DONE]\n\n"))
                .containsExactly("{\"type\":\"response.output_text.delta\",\"delta\":\"答案\"}");
    }

    @Test
    void shouldOnlyPrepareProtocolFallbackForUserProvidedGenericEndpoint() {
        ChatGPTService service = new ChatGPTService();
        LLMRequest custom = new LLMRequest()
                .setBaseUrl("https://relay.example.com/v1")
                .setPublicEndpointOnly(true);
        LLMRequest platform = new LLMRequest()
                .setBaseUrl("https://relay.example.com/v1")
                .setPublicEndpointOnly(false);

        Object customPlan = ReflectionTestUtils.invokeMethod(service, "resolveApiPlan", custom);
        Object platformPlan = ReflectionTestUtils.invokeMethod(service, "resolveApiPlan", platform);
        Object explicitResponsesPlan = ReflectionTestUtils.invokeMethod(service, "resolveApiPlan",
                new LLMRequest().setBaseUrl("https://relay.example.com/v1/responses").setPublicEndpointOnly(true));
        Object explicitChatPlan = ReflectionTestUtils.invokeMethod(service, "resolveApiPlan",
                new LLMRequest().setBaseUrl("https://relay.example.com/v1/chat/completions")
                        .setPublicEndpointOnly(true));

        assertThat(ReflectionTestUtils.getField(customPlan, "fallback")).isNotNull();
        assertThat(ReflectionTestUtils.getField(platformPlan, "fallback")).isNull();
        assertThat(ReflectionTestUtils.getField(explicitResponsesPlan, "fallback")).isNotNull();
        assertThat(ReflectionTestUtils.getField(explicitChatPlan, "fallback")).isNull();
        String responsesEndpoint = ReflectionTestUtils.invokeMethod(
                service, "buildResponsesEndpoint", custom.getBaseUrl());
        String chatCompletionsEndpoint = ReflectionTestUtils.invokeMethod(
                service, "buildChatCompletionsEndpoint", custom.getBaseUrl());
        assertThat(responsesEndpoint)
                .isEqualTo("https://relay.example.com/v1/responses");
        assertThat(chatCompletionsEndpoint)
                .isEqualTo("https://relay.example.com/v1/chat/completions");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildChatCompletionsMultimodalRequestWithoutEnablingReasoning() {
        ChatGPTService service = new ChatGPTService();
        LLMRequest request = new LLMRequest()
                .setModel("gpt-5.6-sol")
                .setSystemPrompt("只回答问题")
                .setThinking(false)
                .setMessages(List.of(LLMMessage.user(List.of(
                        LLMContentPart.text("读取截图"),
                        LLMContentPart.imageUrl("data:image/png;base64,abc", "original")))));

        Mono<Map<String, Object>> bodyMono = ReflectionTestUtils.invokeMethod(
                service, "buildChatCompletionsRequestBody", request, true);
        Map<String, Object> body = bodyMono.block();

        assertThat(body)
                .containsEntry("model", "gpt-5.6-sol")
                .containsEntry("stream", true)
                .containsEntry("reasoning_effort", "none")
                .doesNotContainKey("reasoning");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).containsEntry("role", "system").containsEntry("content", "只回答问题");
        List<Map<String, Object>> content = (List<Map<String, Object>>) messages.get(1).get("content");
        assertThat(content.get(0)).containsEntry("type", "text").containsEntry("text", "读取截图");
        assertThat(content.get(1)).containsEntry("type", "image_url");
    }

    @Test
    void shouldParseChatCompletionsStreamingDeltaAndFinishReason() {
        ChatGPTService service = new ChatGPTService();

        LLMResponse delta = ReflectionTestUtils.invokeMethod(service, "parseChatCompletionsStreamResponse",
                "{\"id\":\"chatcmpl_1\",\"model\":\"gpt-5.6-sol\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"答案\"},\"finish_reason\":null}]}");
        LLMResponse finished = ReflectionTestUtils.invokeMethod(service, "parseChatCompletionsStreamResponse",
                "{\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}");

        assertThat(delta.getChoices().get(0).getDelta().getContent()).isEqualTo("答案");
        assertThat(delta.getFinished()).isFalse();
        assertThat(finished.getFinished()).isTrue();
        assertThat(finished.getChoices().get(0).getFinishReason()).isEqualTo("stop");
    }

    @Test
    void shouldParseResponsesReasoningSummaryAsSummary() {
        ChatGPTService service = new ChatGPTService();

        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parseStreamResponse",
                "{\"type\":\"response.reasoning_summary_text.delta\",\"delta\":\"先分析边界条件\"}");

        assertThat(response.getExtraData())
                .containsEntry("reasoning_content_delta", "先分析边界条件")
                .containsEntry("reasoning_type", "summary");
    }

    @Test
    void shouldParseCompatibleChatReasoningAsContent() {
        ChatGPTService service = new ChatGPTService();

        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parseChatCompletionsStreamResponse",
                "{\"choices\":[{\"index\":0,\"delta\":{\"reasoning_content\":\"逐步推导\"},\"finish_reason\":null}]}");

        assertThat(response.getExtraData())
                .containsEntry("reasoning_content_delta", "逐步推导")
                .containsEntry("reasoning_type", "content");
    }

    @Test
    void shouldParseNonStreamingResponsesReasoningSummary() {
        ChatGPTService service = new ChatGPTService();

        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parseResponse", """
                {"output":[
                  {"type":"reasoning","summary":[{"type":"summary_text","text":"分析动态规划状态"}]},
                  {"type":"message","status":"completed","role":"assistant","content":[{"type":"output_text","text":"答案"}]}
                ]}
                """);

        assertThat(response.getExtraData())
                .containsEntry("reasoning_content", "分析动态规划状态")
                .containsEntry("reasoning_type", "summary");
        assertThat(response.getChoices().get(0).getMessage().getContent()).isEqualTo("答案");
    }

    @Test @SuppressWarnings("unchecked")
    void reasoningRequestsRemoveUnsupportedSamplingAndUseCorrectTokenParameter() {
        var service = new ChatGPTService();
        var request = new LLMRequest().setModel("gpt-6.1-terra").setUserMessage("test").setThinking(true).setTemperature(0.2).setMaxTokens(4096)
            .setExtraParams(Map.of("top_p", 0.9, "top_logprobs", 1, "logprobs", true));
        for (String method : List.of("buildResponsesRequestBody", "buildChatCompletionsRequestBody")) {
            Mono<Map<String,Object>> value = ReflectionTestUtils.invokeMethod(service, method, request, false);
            assertThat(value.block()).doesNotContainKeys("temperature", "top_p", "top_logprobs", "max_tokens");
            if (method.contains("Completions")) assertThat(value.block()).containsEntry("max_completion_tokens",4096).doesNotContainKey("logprobs");
        }
        assertThat(ChatGPTService.supportsDisabledReasoning("gpt-6.1-terra")).isTrue();
        assertThat(ChatGPTService.supportsDisabledReasoning("gpt-6.1-sol")).isFalse();
    }
    @Test void bothProtocolsExposeReasoningUsageForCapabilityChecks() {
        var service = new ChatGPTService();
        LLMResponse responses = ReflectionTestUtils.invokeMethod(service, "parseResponse", "{\"id\":\"r1\",\"output\":[],\"usage\":{\"input_tokens\":1,\"output_tokens\":4,\"output_tokens_details\":{\"reasoning_tokens\":0}}}");
        LLMResponse chat = ReflectionTestUtils.invokeMethod(service, "parseChatCompletionsResponse", "{\"id\":\"r2\",\"choices\":[],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":4,\"completion_tokens_details\":{\"reasoning_tokens\":3}}}");
        assertThat(responses.getExtraData()).containsEntry("reasoning_tokens",0L);
        assertThat(chat.getExtraData()).containsEntry("reasoning_tokens",3L);
    }
    @Test @SuppressWarnings("unchecked") void chatFallbackUsesSupportedImageDetail() {
        var service = new ChatGPTService();
        Map<String,Object> content = ReflectionTestUtils.invokeMethod(service, "buildChatContentPart", LLMContentPart.imageUrl("data:image/png;base64,abc", "original"));
        assertThat((Map<String,Object>)content.get("image_url")).containsEntry("detail", "high");
    }
}
