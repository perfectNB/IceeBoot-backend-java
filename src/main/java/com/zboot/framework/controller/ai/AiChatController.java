/*
 * @Description: AI对话管理控制器
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.zboot.framework.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.R;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.create.ai.AiChatSessionDTO;
import com.zboot.framework.dto.create.ai.AiChatMessageDTO;
import com.zboot.framework.dto.create.ai.StreamChatRequest;
import com.zboot.framework.query.ai.AiChatSessionQuery;
import com.zboot.framework.query.ai.AiChatMessageQuery;
import com.zboot.framework.service.ai.IAiChatService;
import com.zboot.framework.service.ai.IStreamChatService;
import com.zboot.framework.vo.ai.AiChatSessionVO;
import com.zboot.framework.vo.ai.AiChatMessageVO;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    private final IStreamChatService streamChatService;


    @Operation(summary = "获取对话会话列表")
    @GetMapping("/sessions")
//    @RequirePermission(dynamic = true)
    public R<IPage<AiChatSessionVO>> getSessionPage(AiChatSessionQuery query) {
        IPage<AiChatSessionVO> page = aiChatService.getSessionPage(query);
        return R.ok(page);
    }


    @Operation(summary = "创建对话会话")
    @PostMapping("/sessions")
//    @RequirePermission(dynamic = true)
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
        return streamChatService.streamChat(request)
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


}