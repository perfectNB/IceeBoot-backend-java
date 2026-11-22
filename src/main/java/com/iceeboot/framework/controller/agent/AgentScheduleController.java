package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.agent.AgentScheduleDTO;
import com.iceeboot.framework.dto.update.agent.*;
import com.iceeboot.framework.service.agent.IAgentScheduleService;
import com.iceeboot.framework.service.agent.schedule.AgentScheduleAccess;
import com.iceeboot.framework.vo.agent.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "智能体定时任务")
@RequestMapping("/v1/agent-console/schedules")
public class AgentScheduleController {
    private final IAgentScheduleService schedules;
    private final AgentScheduleAccess access;

    private long owner() {
        Long id = TokenUtils.getCurrentUserId();
        access.requireAdmin(id);
        return id;
    }

    @GetMapping
    @Operation(summary = "查询智能体定时任务")
    @RequirePermission(dynamic = true)
    public R<List<AgentScheduleVO>> list() { return R.ok(schedules.list(owner())); }

    @PostMapping
    @Operation(summary = "创建智能体定时任务")
    @RequirePermission(dynamic = true)
    public R<AgentScheduleVO> create(@Valid @RequestBody AgentScheduleDTO request) {
        return R.ok(schedules.create(owner(), request));
    }

    @PutMapping
    @Operation(summary = "更新智能体定时任务")
    @RequirePermission(dynamic = true)
    public R<AgentScheduleVO> update(@Valid @RequestBody AgentScheduleUpdateDTO request) {
        return R.ok(schedules.update(owner(), request));
    }

    @PutMapping("/{id}/enabled")
    @Operation(summary = "启停智能体定时任务")
    @RequirePermission(dynamic = true)
    public R<AgentScheduleVO> enabled(@PathVariable long id, @Valid @RequestBody AgentScheduleStatusDTO request) {
        return R.ok(schedules.enabled(owner(), id, request.enabled()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除智能体定时任务")
    @RequirePermission(dynamic = true)
    public R<Void> delete(@PathVariable long id) {
        schedules.delete(owner(), id);
        return R.ok();
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "查询智能体定时执行记录")
    @RequirePermission(dynamic = true)
    public R<List<AgentScheduleExecutionVO>> executions(@PathVariable long id,
            @RequestParam(defaultValue = "0") int offset) {
        return R.ok(schedules.executions(owner(), id, offset));
    }
}
