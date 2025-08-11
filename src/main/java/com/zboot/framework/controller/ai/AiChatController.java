/*
 * @Description: AI对话管理控制器
 * @Date: 2025-01-27
 * @Author: CodeIcee
 */
package com.zboot.framework.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.ai.AiChatSessionDTO;
import com.zboot.framework.dto.ai.AiChatMessageDTO;
import com.zboot.framework.query.ai.AiChatSessionQuery;
import com.zboot.framework.service.ai.IAiChatService;
import com.zboot.framework.vo.ai.AiChatSessionVO;
import com.zboot.framework.vo.ai.AiChatMessageVO;
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
 * @since 2025-01-27
 */
@Tag(name = "AI对话管理", description = "AI对话相关接口")
@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final IAiChatService aiChatService;

    @Operation(summary = "获取对话会话列表")
    @GetMapping("/sessions")
    public R<IPage<AiChatSessionVO>> getSessionPage(AiChatSessionQuery query) {
        IPage<AiChatSessionVO> page = aiChatService.getSessionPage(query);
        return R.ok(page);
    }

    @Operation(summary = "获取对话会话详情")
    @GetMapping("/sessions/{id}")
    public R<AiChatSessionVO> getSessionById(@Parameter(description = "会话ID") @PathVariable Long id) {
        AiChatSessionVO session = aiChatService.getSessionById(id);
        return R.ok(session);
    }

    @Operation(summary = "创建对话会话")
    @PostMapping("/sessions")
    public R<AiChatSessionVO> addSession(@Validated @RequestBody AiChatSessionDTO dto) {
        Boolean result = aiChatService.addSession(dto);
        if (result) {
            return R.ok();
        }
        return R.fail("创建失败");
    }

    @Operation(summary = "删除对话会话")
    @DeleteMapping("/sessions/{id}")
    public R<Void> deleteSession(@Parameter(description = "会话ID") @PathVariable Long id) {
        Boolean result = aiChatService.deleteSession(List.of(id));
        if (result) {
            return R.ok();
        }
        return R.fail("删除失败");
    }

    @Operation(summary = "发送消息")
    @PostMapping("/sessions/{sessionId}/messages")
    public R<AiChatMessageVO> sendMessage(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestBody Map<String, String> request) {
        AiChatMessageDTO dto = new AiChatMessageDTO();
        dto.setSessionId(sessionId);
        dto.setContent(request.get("content"));
        AiChatMessageVO message = aiChatService.sendMessage(dto);
        return R.ok(message);
    }

    @Operation(summary = "获取可用的大模型列表")
    @GetMapping("/models")
    public R<List<Object>> getAvailableModels() {
        // TODO: 实现获取可用模型列表
        return R.ok(List.of());
    }

    @Operation(summary = "获取模型API配置")
    @GetMapping("/models/{modelName}/config")
    public R<Map<String, String>> getModelConfig(@Parameter(description = "模型名称") @PathVariable String modelName) {
        // TODO: 实现获取模型配置
        return R.ok(Map.of());
    }

    @Operation(summary = "流式对话")
    @PostMapping("/stream")
    public R<Void> streamChat(@RequestBody Map<String, Object> request) {
        // TODO: 实现流式对话
        return R.ok();
    }
}