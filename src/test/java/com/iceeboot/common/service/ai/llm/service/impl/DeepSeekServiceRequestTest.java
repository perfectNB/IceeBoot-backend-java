package com.iceeboot.common.service.ai.llm.service.impl;

import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.dto.LLMContentPart;
import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.config.LlmModelRoutingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekServiceRequestTest {

    private DeepSeekService service;

    @BeforeEach
    void setUp() {
        service = new DeepSeekService();
        LlmModelRoutingProperties routing = new LlmModelRoutingProperties();
        routing.setModelAliases(Map.of("deepseek", Map.of(
                "deepseek-v4pro", "deepseek-v4-pro"
        )));
        service.setModelRoutingProperties(routing);
    }

    @Test
    void shouldDisableThinkingForDeepSeekV4ProByDefault() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-v4-pro")
                .setUserMessage("普通创作"));

        assertThat(body)
                .containsEntry("model", "deepseek-v4-pro")
                .containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void shouldPreserveExplicitlyEnabledThinkingForDeepSeekV4Pro() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-v4pro")
                .setUserMessage("消痕改写")
                .setThinking(true));

        assertThat(body)
                .containsEntry("model", "deepseek-v4-pro")
                .containsEntry("thinking", Map.of("type", "enabled"));
    }

    @Test
    void shouldMapExtremeThinkingToDeepSeekMaxWithoutReplacingCustomModel() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-flash")
                .setPublicEndpointOnly(true)
                .setUserMessage("做题")
                .setThinking(true)
                .setReasoningEffort("xhigh"));

        assertThat(body)
                .containsEntry("model", "deepseek-flash")
                .containsEntry("thinking", Map.of("type", "enabled"))
                .containsEntry("reasoning_effort", "max");
    }

    @Test
    void shouldRejectReasonerWhenThinkingIsDisabled() {
        Mono<Map<String, Object>> body = ReflectionTestUtils.invokeMethod(service, "buildRequestBody",
                new LLMRequest().setModel("deepseek-reasoner").setUserMessage("请求").setThinking(false), false);

        assertThatThrownBy(body::block).hasMessageContaining("不支持关闭思考");
    }

    @Test
    void shouldNotSendV4ThinkingParametersToFixedReasoner() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-reasoner")
                .setPublicEndpointOnly(true)
                .setUserMessage("做题")
                .setThinking(true)
                .setReasoningEffort("xhigh"));

        assertThat(body)
                .containsEntry("model", "deepseek-reasoner")
                .doesNotContainKeys("thinking", "reasoning_effort");
    }

    @Test
    void shouldDisableThinkingForFlashWhenRequestDisablesIt() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-v4-flash")
                .setUserMessage("示例正文")
                .setThinking(false));

        assertThat(body)
                .containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void shouldNotAddThinkingForOtherDeepSeekModels() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-chat")
                .setUserMessage("普通请求"));

        assertThat(body).doesNotContainKey("thinking");
    }

    @Test
    void shouldSplitCombinedSseEventsWithoutDroppingPayloads() {
        List<String> payloads = service.extractStreamPayloads("data: {\"id\":\"1\"}\n\n"
                + "data: {\"id\":\"2\"}\n\ndata: [DONE]\n\n");

        assertThat(payloads).containsExactly("{\"id\":\"1\"}", "{\"id\":\"2\"}");
    }

    @Test
    void shouldParseDeltaAndMessageContentFromStream() {
        LLMResponse deltaResponse = ReflectionTestUtils.invokeMethod(service, "parseStreamResponse",
                "{\"choices\":[{\"index\":0,\"delta\":{\"content\":\"第一段\"}}]}");
        LLMResponse messageResponse = ReflectionTestUtils.invokeMethod(service, "parseStreamResponse",
                "{\"choices\":[{\"index\":0,\"message\":{\"content\":\"第二段\"}}]}");

        assertThat(deltaResponse).isNotNull();
        assertThat(deltaResponse.getChoices().get(0).getDelta().getContent()).isEqualTo("第一段");
        assertThat(messageResponse).isNotNull();
        assertThat(messageResponse.getChoices().get(0).getMessage().getContent()).isEqualTo("第二段");
    }

    @Test
    void shouldMarkDeepSeekReasoningAsContent() {
        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parseStreamResponse",
                "{\"choices\":[{\"index\":0,\"delta\":{\"reasoning_content\":\"先枚举边界\"}}]}");

        assertThat(response.getExtraData())
                .containsEntry("reasoning_content_delta", "先枚举边界")
                .containsEntry("reasoning_type", "content");
    }

    @Test
    void shouldParseTopLevelOutputTextDelta() {
        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parseStreamResponse",
                "{\"type\":\"response.output_text.delta\",\"delta\":\"正文分片\"}");

        assertThat(response).isNotNull();
        assertThat(response.getChoices().get(0).getDelta().getContent()).isEqualTo("正文分片");
    }

    @Test
    void shouldTranslateEnabledThinkingForDashScopeCompatibleApi() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-v4-pro")
                .setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .setUserMessage("示例正文")
                .setThinking(true));

        assertThat(body)
                .containsEntry("enable_thinking", true)
                .doesNotContainKey("thinking");
    }

    @Test
    void shouldTranslateDisabledThinkingForDashScopeCompatibleApi() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-v4-pro")
                .setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .setUserMessage("普通请求"));

        assertThat(body)
                .containsEntry("enable_thinking", false)
                .doesNotContainKey("thinking");
    }

    @Test
    void shouldAcceptBaseOrCompleteChatEndpoint() {
        assertThat((String) ReflectionTestUtils.invokeMethod(service, "buildChatEndpoint",
                "https://api.deepseek.com"))
                .isEqualTo("https://api.deepseek.com/chat/completions");
        assertThat((String) ReflectionTestUtils.invokeMethod(service, "buildChatEndpoint",
                "https://api.deepseek.com/chat/completions/"))
                .isEqualTo("https://api.deepseek.com/chat/completions");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildOfficialVisionContentBlocks() {
        Map<String, Object> body = buildRequestBody(new LLMRequest()
                .setModel("deepseek-v4-flash-vision-exp")
                .setMessages(List.of(
                        LLMMessage.system("直接根据截图回答"),
                        LLMMessage.user(List.of(
                                LLMContentPart.text("使用 Java"),
                                LLMContentPart.imageUrl("data:image/png;base64,AQID",
                                        LLMContentPart.IMAGE_DETAIL_ORIGINAL))))));

        assertThat(body).containsEntry("model", "deepseek-v4-flash-vision-exp");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).containsEntry("role", "system").containsEntry("content", "直接根据截图回答");
        assertThat(messages.get(1).get("role")).isEqualTo("user");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) messages.get(1).get("content");
        assertThat(parts).hasSize(2);
        assertThat(parts.get(0)).containsEntry("type", "text").containsEntry("text", "使用 Java");
        assertThat((Map<String, Object>) parts.get(1).get("image_url"))
                .containsEntry("url", "data:image/png;base64,AQID")
                .containsEntry("detail", "original");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Mono<Map<String, Object>> bodyMono = ReflectionTestUtils.invokeMethod(
                service, "buildRequestBody", request, false);
        assertThat(bodyMono).isNotNull();
        return bodyMono.block();
    }
}
