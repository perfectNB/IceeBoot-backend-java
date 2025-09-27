/*
 * @Description: AI对话管理控制器
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.iceeboot.framework.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iceeboot.common.annotation.RateLimit;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.enums.RateLimitType;
import com.iceeboot.framework.dto.create.ai.AiChatSessionDTO;
import com.iceeboot.framework.dto.create.ai.StreamChatRequest;
import com.iceeboot.framework.dto.create.ai.AiChatMessageDTO;
import com.iceeboot.framework.query.ai.AiChatSessionQuery;
import com.iceeboot.framework.query.ai.AiChatMessageQuery;
import com.iceeboot.framework.service.ai.IAiChatService;
import com.iceeboot.common.utils.tool.JapanMovieActorTool;
import com.iceeboot.common.utils.tool.JapanMoviePathTool;
import com.iceeboot.framework.vo.ai.AiChatSessionVO;
import com.iceeboot.framework.vo.ai.AiChatMessageVO;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI对话管理控制器
 *
 * @author CodeIcee
 * @since 2025-08-11
 */
@Tag(name = "AI对话管理", description = "AI对话相关接口")
@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final IAiChatService aiChatService;

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;


    @Operation(summary = "获取对话会话列表")
    @GetMapping("/sessions")
    @RequirePermission(dynamic = true)
    public R<IPage<AiChatSessionVO>> getSessionPage(AiChatSessionQuery query) {
        IPage<AiChatSessionVO> page = aiChatService.getSessionPage(query);
        return R.ok(page);
    }


    @Operation(summary = "创建对话会话")
    @PostMapping("/sessions")
    @RequirePermission(dynamic = true)
    public R<AiChatSessionVO> addSession(@Validated @RequestBody AiChatSessionDTO dto) {
        Boolean result = aiChatService.addSession(dto);
        if (result) {
            return R.ok();
        }
        return R.fail(ResponseMessage.CREATE_FAIL);
    }


    @Operation(summary = "删除对话会话", description = "单个删除或批量删除对话")
    @DeleteMapping
    public R<Void> deleteLogs(@RequestBody List<Long> ids) {
        boolean success = aiChatService.deleteSession(ids);
        return success ? R.ok() : R.fail(ResponseMessage.DELETE_FAIL);
    }

    @Operation(summary = "流式对话")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    @RequirePermission(dynamic = true)
    public Flux<String> streamChat(@Validated @RequestBody StreamChatRequest request) {
        return aiChatService.streamChat(request)
                .map(response -> {
                    try {
                        // 将LLMResponse转换为SSE格式
                        if (response.getError() != null) {
                            return "data: {\"error\":\"" + response.getError() + "\",\"finished\":true}\n\n";
                        }

                        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                            var choice = response.getChoices().get(0);
                            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                String content = choice.getDelta().getContent().replace("\"", "\\\"").replace("\n", "\\n");
                                return "data: {\"content\":\"" + content + "\",\"finished\":false}\n\n";
                            }
                        }

                        if (Boolean.TRUE.equals(response.getFinished())) {
                            return "data: {\"finished\":true}\n\n";
                        }

                        return "data: {\"heartbeat\":true}\n\n";
                    } catch (Exception e) {
                        return "data: {\"error\":\"数据处理异常\",\"finished\":true}\n\n";
                    }
                })
                .onErrorReturn("data: {\"error\":\"流式对话异常\",\"finished\":true}\n\n");
    }

    @Operation(summary = "获取对话里的消息")
    @GetMapping("/message")
//    @RequirePermission(dynamic = true)
    public R<PageResult<AiChatMessageVO>> getMessage(AiChatMessageQuery query) {
        PageResult<AiChatMessageVO> result = aiChatService.getMessage(query);
        return R.ok(result);
    }

    @Operation(summary = "工具调用流式对话")
    @PostMapping(value = "/tool/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    @RequirePermission(dynamic = true)
    public Flux<String> toolStreamChat(@Validated @RequestBody StreamChatRequest request) {
        // 保存用户消息
        AiChatMessageDTO userMessageDto = new AiChatMessageDTO();
        userMessageDto.setSessionId(request.getSessionId());
        userMessageDto.setRole("user");
        userMessageDto.setContent(request.getContent());
        aiChatService.sendMessage(userMessageDto);

        // 使用工具调用的ChatClient进行流式对话，直接返回内容流
        return qwenChatClient.prompt()
                .user(request.getContent())
                .tools(new JapanMoviePathTool(), new JapanMovieActorTool())
                .stream()
                .content()
                .collectList() // 收集所有内容片段用于保存
                .doOnNext(contentList -> {
                    // 保存AI回复消息
                    String fullResponse = String.join("", contentList);
                    if (!fullResponse.trim().isEmpty()) {
                        AiChatMessageDTO aiMessageDto = new AiChatMessageDTO();
                        aiMessageDto.setSessionId(request.getSessionId());
                        aiMessageDto.setRole("assistant");
                        aiMessageDto.setContent(fullResponse);
                        aiChatService.sendMessage(aiMessageDto);
                    }
                })
                .flatMapMany(Flux::fromIterable); // 重新转换为流式响应
    }


}