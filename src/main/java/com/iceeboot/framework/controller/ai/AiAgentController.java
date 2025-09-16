package com.iceeboot.framework.controller.ai;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.ai.AiAgentDTO;
import com.iceeboot.framework.dto.update.ai.AiAgentUpdateDTO;
import com.iceeboot.framework.query.ai.AiAgentQuery;
import com.iceeboot.framework.service.ai.IAiAgentService;
import com.iceeboot.framework.vo.ai.AiAgentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI代理管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "Ai Agent管理", description = "AI Agent管理相关接口")
@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
public class AiAgentController {

    private final IAiAgentService aiAgentService;

    @Operation(summary = "获取agents列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<PageResult<AiAgentVO>> getAgentPage(AiAgentQuery query) {
        PageResult<AiAgentVO> result = aiAgentService.getAgentPage(query);
        return R.ok(result);
    }


    @Operation(summary = "创建agents")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<AiAgentVO> addAgent(@Validated @RequestBody AiAgentDTO dto) {
        Boolean result = aiAgentService.addAgent(dto);
        if (result) {
            return R.ok();
        }
        return R.fail(ResponseMessage.CREATE_FAIL);
    }

    @Operation(summary = "更新agents", description = "更新agents信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateAgent(@Validated @RequestBody AiAgentUpdateDTO dto) {
        Boolean result = aiAgentService.updateAgent(dto);
        return result ? R.ok() : R.fail(ResponseMessage.UPDATE_FAIL);
    }

    @Operation(summary = "删除agents")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteAgent(@RequestBody List<Long> ids) {
        Boolean result = aiAgentService.deleteAgent(ids);
        return result ? R.ok() : R.fail(ResponseMessage.DELETE_FAIL);
    }

    @Operation(summary = "更新代理状态")
    @PutMapping("/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateAgentStatus(
            @Parameter(description = "代理ID") @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {
        Boolean result = aiAgentService.updateAgentStatus(id, request.get("status"));
        if (result) {
            return R.ok();
        }
        return R.fail(ResponseMessage.STATUS_UPDATE_FAIL_AI);
    }

    @Operation(summary = "根据平台获取代理列表")
    @GetMapping("/platform/{platform}")
    @RequirePermission(dynamic = true)
    public R<List<AiAgentVO>> getAgentsByPlatform(@Parameter(description = "平台") @PathVariable("platform") String platform) {
        List<AiAgentVO> agents = aiAgentService.getAgentsByPlatform(platform);
        return R.ok(agents);
    }

    @Operation(summary = "根据类型获取代理列表")
    @GetMapping("/type/{type}")
    @RequirePermission(dynamic = true)
    public R<List<AiAgentVO>> getAgentsByType(@Parameter(description = "类型") @PathVariable("type") String type) {
        List<AiAgentVO> agents = aiAgentService.getAgentsByType(type);
        return R.ok(agents);
    }
}