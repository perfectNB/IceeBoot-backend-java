package com.iceeboot.common.service.ai.llm.service.impl;

import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.dto.LLMContentPart;
import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZhipuServiceRequestTest {

    private final ZhipuService service = new ZhipuService();

    @Test
    void shouldRejectOrdinaryGlm53RequestBecauseThinkingCannotBeDisabled() {
        LLMRequest request = new LLMRequest()
                .setModel("glm-5.3")
                .setUserMessage("你好")
                .setThinking(false)
                .setReasoningEffort("none");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> requestBody(request))
                .hasMessageContaining("不支持关闭思考");
    }

    @Test
    void shouldUseRequestedThinkingLevelForGlm53ThinkingShortcut() {
        Map<String, Object> body = requestBody(new LLMRequest()
                .setModel("GLM-5.3-FlashX")
                .setUserMessage("你好")
                .setThinking(true)
                .setReasoningEffort("high"));

        assertThat(body)
                .containsEntry("thinking", Map.of("type", "enabled"))
                .containsEntry("reasoning_effort", "high");
    }

    @Test
    void shouldMapExtremeGlm53ThinkingToVendorMax() {
        Map<String, Object> body = requestBody(new LLMRequest()
                .setModel("glm-5.3")
                .setUserMessage("你好")
                .setThinking(true)
                .setReasoningEffort("xhigh"));

        assertThat(body).containsEntry("reasoning_effort", "max");
    }

    @Test
    void shouldStillDisableThinkingForModelsThatSupportIt() {
        Map<String, Object> body = requestBody(new LLMRequest()
                .setModel("glm-5.2")
                .setUserMessage("你好")
                .setThinking(false)
                .setReasoningEffort("none"));

        assertThat(body)
                .containsEntry("thinking", Map.of("type", "disabled"))
                .doesNotContainKey("reasoning_effort");
    }

    @Test
    void shouldNotSendUnsupportedEffortToOlderGlmModels() {
        Map<String, Object> body = requestBody(new LLMRequest()
                .setModel("glm-4.7")
                .setUserMessage("你好")
                .setThinking(true)
                .setReasoningEffort("xhigh"));

        assertThat(body)
                .containsEntry("thinking", Map.of("type", "enabled"))
                .doesNotContainKey("reasoning_effort");
    }

    @Test
    void shouldKeepVendorDefaultsWhenGlm53ModeWasNotSpecified() {
        Map<String, Object> body = requestBody(new LLMRequest()
                .setModel("glm-5.3")
                .setUserMessage("你好"));

        assertThat(body).doesNotContainKeys("thinking", "reasoning_effort");
    }

    @Test
    void shouldMarkGlmReasoningAsContent() {
        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parse",
                "{\"choices\":[{\"index\":0,\"delta\":{\"reasoning_content\":\"先分析复杂度\"},\"finish_reason\":null}]}", true);

        assertThat(response.getExtraData())
                .containsEntry("reasoning_content_delta", "先分析复杂度")
                .containsEntry("reasoning_type", "content");
    }

    @Test
    void shouldSendVisionInputInTheGlmImageUrlFormat() {
        LLMRequest request = new LLMRequest()
                .setModel("glm-4.6v")
                .setMessages(List.of(LLMMessage.user(List.of(
                        LLMContentPart.text("Describe this image"),
                        LLMContentPart.imageUrl("data:image/png;base64,dGVzdA==", "original")))));

        Map<String, Object> body = requestBody(request);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) messages.get(0).get("content");

        assertThat(content.get(1)).containsEntry("type", "image_url")
                .containsEntry("image_url", Map.of("url", "data:image/png;base64,dGVzdA=="));
    }

    @Test
    void shouldReadTheFinalTextFromANonStreamingGlmResponse() {
        LLMResponse response = ReflectionTestUtils.invokeMethod(service, "parse",
                "{\"model\":\"glm-4.6v\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"reasoning_content\":\"先观察图片\",\"content\":\"图片里有四位数字\"},\"finish_reason\":\"stop\"}]}",
                false);

        assertThat(response.getChoices().get(0).getMessage().getContent()).isEqualTo("图片里有四位数字");
        assertThat(response.getChoices().get(0).getFinishReason()).isEqualTo("stop");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestBody(LLMRequest request) {
        return ReflectionTestUtils.invokeMethod(service, "requestBody", request, false);
    }
}
