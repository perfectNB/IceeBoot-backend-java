/*
 * @Description: AI大模型管理控制器
 * @Date: 2025-01-27
 * @Author: CodeIcee
 */
package com.zboot.framework.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.ai.AiLLMDTO;
import com.zboot.framework.query.ai.AiLLMQuery;
import com.zboot.framework.service.ai.IAiLLMService;
import com.zboot.framework.vo.ai.AiLLMVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI大模型管理控制器
 *
 * @author CodeIcee
 * @since 2025-08-11
 */
@Tag(name = "AI大模型管理", description = "AI大模型相关接口")
@RestController
@RequestMapping("/v1/llm")
@RequiredArgsConstructor
public class AiLLMController {

    private final IAiLLMService aiLLMService;

    @Operation(summary = "获取大模型列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<IPage<AiLLMVO>> getLLMPage(AiLLMQuery query) {
        IPage<AiLLMVO> page = aiLLMService.getLLMPage(query);
        return R.ok(page);
    }

    @Operation(summary = "根据ID获取大模型")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<AiLLMVO> getLLMById(@Parameter(description = "大模型ID") @PathVariable Long id) {
        AiLLMVO llm = aiLLMService.getLLMById(id);
        return R.ok(llm);
    }

    @Operation(summary = "创建大模型")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<AiLLMVO> addLLM(@Validated @RequestBody AiLLMDTO dto) {
        Boolean result = aiLLMService.addLLM(dto);
        if (result) {
            return R.ok();
        }
        return R.fail(ResponseMessage.CREATE_FAIL);
    }

    @Operation(summary = "更新大模型")
    @PutMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<AiLLMVO> updateLLM(
            @Parameter(description = "大模型ID") @PathVariable Long id,
            @Validated @RequestBody AiLLMDTO dto) {
        dto.setId(id);
        Boolean result = aiLLMService.updateLLM(dto);
        if (result) {
            return R.ok();
        }
        return R.fail(ResponseMessage.UPDATE_FAIL);
    }

    @Operation(summary = "删除大模型")
    @DeleteMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteLLM(@Parameter(description = "大模型ID") @PathVariable Long id) {
        Boolean result = aiLLMService.deleteLLM(List.of(id));
        if (result) {
            return R.ok();
        }
        return R.fail(ResponseMessage.DELETE_FAIL);
    }

    @Operation(summary = "添加大模型密钥")
    @PostMapping("/{modelId}/keys")
    @RequirePermission(dynamic = true)
    public R<Void> addLLMKey(
            @Parameter(description = "大模型ID") @PathVariable Long modelId,
            @RequestBody Map<String, Object> keyData) {
        // TODO: 实现添加密钥逻辑
        return R.ok();
    }

    @Operation(summary = "删除大模型密钥")
    @DeleteMapping("/{modelId}/keys/{keyId}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteLLMKey(
            @Parameter(description = "大模型ID") @PathVariable Long modelId,
            @Parameter(description = "密钥ID") @PathVariable Long keyId) {
        // TODO: 实现删除密钥逻辑
        return R.ok();
    }

    @Operation(summary = "更新大模型密钥状态")
    @PutMapping("/{modelId}/keys/{keyId}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateLLMKeyStatus(
            @Parameter(description = "大模型ID") @PathVariable Long modelId,
            @Parameter(description = "密钥ID") @PathVariable Long keyId,
            @RequestBody Map<String, String> request) {
        // TODO: 实现更新密钥状态逻辑
        return R.ok();
    }
}