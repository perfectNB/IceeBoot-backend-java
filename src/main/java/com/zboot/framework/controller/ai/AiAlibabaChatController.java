package com.zboot.framework.controller.ai;

import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.create.ai.StreamChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 基于 Spring AI Alibaba 的通义千问对话控制器
 * 保留原有 WebFlux 对话方式不变，新增直连 ChatModel 的对话能力
 */
@RestController
@RequestMapping("/v1/chat/ali")
@RequiredArgsConstructor
public class AiAlibabaChatController {

    private final ChatModel chatModel;

    /**
     * 非流式对话（直接返回完整回复）
     */
    @PostMapping("/chat")
    public R<String> chat(@Validated @RequestBody StreamChatRequest request) {
        String userInput = request.getContent();
        if (userInput == null || userInput.isBlank()) {
            return R.fail(ResponseMessage.AI_CHAT_CONTENT_EMPTY);
        }
        String content = chatModel
                .call(new Prompt(userInput))
                .getResult()
                .getOutput()
                .getContent();
        return R.ok(content);
    }

    /**
     * 流式对话（SSE）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@Validated @RequestBody StreamChatRequest request) {
        String userInput = request.getContent();
        if (userInput == null || userInput.isBlank()) {
            return Flux.just("data: {\"error\":\"对话内容不能为空\",\"finished\":true}\n\n");
        }

        Flux<ChatResponse> stream = chatModel.stream(new Prompt(userInput));
        return stream
                .map(resp -> {
                    String piece = resp.getResult().getOutput().getContent();
                    if (piece == null) piece = "";
                    String escaped = piece.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                    return "data: {\"content\":\"" + escaped + "\",\"finished\":false}\n\n";
                })
                .onErrorResume(e -> Mono.just("data: {\"error\":\"" + e.getMessage().replace("\"", "'") + "\",\"finished\":true}\n\n"))
                .concatWith(Mono.just("data: {\"finished\":true}\n\n"));
    }
}