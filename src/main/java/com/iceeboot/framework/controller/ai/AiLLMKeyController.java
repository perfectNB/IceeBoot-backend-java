/*
 * @Description: AI大模型密钥管理控制器
 * @Date: 2025-09-04
 * @Author: CodeIcee
 */
package com.iceeboot.framework.controller.ai;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.ai.AiLLMKeyDTO;
import com.iceeboot.framework.dto.update.ai.AiLLMKeyUpdateDTO;
import com.iceeboot.framework.service.ai.IAiLLMKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.iceeboot.framework.vo.ai.AiLLMProviderVO;

import java.util.List;
import java.util.Map;

/**
 * AI大模型密钥管理控制器
 *
 * @author CodeIcee
 * @since 2025-09-04
 */
@Tag(name = "AI大模型密钥管理", description = "AI大模型密钥相关接口")
@RestController
@RequestMapping("/v1/llm-keys")
@RequiredArgsConstructor
public class AiLLMKeyController {

    private final IAiLLMKeyService aiLLMKeyService;

    @Operation(summary = "获取所有厂商的密钥列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<List<AiLLMProviderVO>> getAllProviderKeys() {
        List<AiLLMProviderVO> result = aiLLMKeyService.getAllProviderKeys();
        return R.ok(result);
    }




    @Operation(summary = "新增大模型密钥")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addLLMKey(@Validated @RequestBody AiLLMKeyDTO dto) {
        Boolean result = aiLLMKeyService.addLLMKey(dto);
        return result ? R.ok() : R.fail(ResponseMessage.CREATE_FAIL);
    }

    @Operation(summary = "更新大模型密钥", description = "更新大模型密钥信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateLLMKey(@Validated @RequestBody AiLLMKeyUpdateDTO dto) {
        Boolean result = aiLLMKeyService.updateLLMKey(dto);
        return result ? R.ok() : R.fail(ResponseMessage.UPDATE_FAIL);
    }

    @Operation(summary = "删除大模型密钥")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteLLMKey(@RequestBody List<Long> ids) {
        Boolean result = aiLLMKeyService.deleteLLMKey(ids);
        return result ? R.ok() : R.fail(ResponseMessage.DELETE_FAIL);
    }

    @Operation(summary = "更新大模型密钥状态")
    @PutMapping("/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateLLMKeyStatus(
            @Parameter(description = "密钥ID") @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Boolean result = aiLLMKeyService.updateLLMKeyStatus(id, status);
        return result ? R.ok() : R.fail(ResponseMessage.STATUS_UPDATE_FAIL);
    }




    @Operation(summary = "批量导入密钥")
    @PostMapping("/batch-import")
    @RequirePermission(dynamic = true)
    public R<Integer> batchImportKeys(@RequestBody List<AiLLMKeyDTO> keys) {
        Integer result = aiLLMKeyService.batchImportKeys(keys);
        return R.ok(result);
    }

    @Operation(summary = "获取密钥使用统计")
    @GetMapping("/stats")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getKeyUsageStats(
            @Parameter(description = "大模型厂商（可选）") @RequestParam(required = false) String provider) {
        Map<String, Object> result = aiLLMKeyService.getKeyUsageStats(provider);
        return R.ok(result);
    }





}