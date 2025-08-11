package com.zboot.framework.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.ai.AiAgentDTO;
import com.zboot.framework.query.ai.AiAgentQuery;
import com.zboot.framework.service.ai.IAiAgentService;
import com.zboot.framework.vo.ai.AiAgentVO;
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
@Tag(name = "AI代理管理", description = "AI代理管理相关接口")
@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
public class AiAgentController {

    private final IAiAgentService aiAgentService;

    @Operation(summary = "获取代理列表")
    @GetMapping
    public R<IPage<AiAgentVO>> getAgentPage(AiAgentQuery query) {
        IPage<AiAgentVO> page = aiAgentService.getAgentPage(query);
        return R.ok(page);
    }

    @Operation(summary = "根据ID获取代理")
    @GetMapping("/{id}")
    public R<AiAgentVO> getAgentById(@Parameter(description = "代理ID") @PathVariable Long id) {
        AiAgentVO agent = aiAgentService.getAgentById(id);
        return R.ok(agent);
    }

    @Operation(summary = "创建代理")
    @PostMapping
    public R<AiAgentVO> addAgent(@Validated @RequestBody AiAgentDTO dto) {
        Boolean result = aiAgentService.addAgent(dto);
        if (result) {
            return R.ok();
        }
        return R.fail("创建失败");
    }

    @Operation(summary = "更新代理")
    @PutMapping("/{id}")
    public R<AiAgentVO> updateAgent(
            @Parameter(description = "代理ID") @PathVariable Long id,
            @Validated @RequestBody AiAgentDTO dto) {
        dto.setId(id);
        Boolean result = aiAgentService.updateAgent(dto);
        if (result) {
            return R.ok();
        }
        return R.fail("更新失败");
    }

    @Operation(summary = "删除代理")
    @DeleteMapping("/{id}")
    public R<Void> deleteAgent(@Parameter(description = "代理ID") @PathVariable Long id) {
        Boolean result = aiAgentService.deleteAgent(List.of(id));
        if (result) {
            return R.ok();
        }
        return R.fail("删除失败");
    }

    @Operation(summary = "更新代理状态")
    @PutMapping("/{id}/status")
    public R<Void> updateAgentStatus(
            @Parameter(description = "代理ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Boolean result = aiAgentService.updateAgentStatus(id, request.get("status"));
        if (result) {
            return R.ok();
        }
        return R.fail("状态更新失败");
    }

    @Operation(summary = "根据平台获取代理列表")
    @GetMapping("/platform/{platform}")
    public R<List<AiAgentVO>> getAgentsByPlatform(@Parameter(description = "平台") @PathVariable String platform) {
        List<AiAgentVO> agents = aiAgentService.getAgentsByPlatform(platform);
        return R.ok(agents);
    }

    @Operation(summary = "根据类型获取代理列表")
    @GetMapping("/type/{type}")
    public R<List<AiAgentVO>> getAgentsByType(@Parameter(description = "类型") @PathVariable String type) {
        List<AiAgentVO> agents = aiAgentService.getAgentsByType(type);
        return R.ok(agents);
    }
}