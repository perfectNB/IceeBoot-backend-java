package com.zboot.framework.controller.system;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.entity.TaskDO;
import com.zboot.framework.query.system.TaskQuery;
import com.zboot.framework.service.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Tag(name = "定时任务管理", description = "定时任务管理相关接口")
@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;

    /**
     * 分页查询任务列表
     */
    @Operation(summary = "分页查询任务列表", description = "根据条件分页查询任务列表")
    @GetMapping("/page")
    public R<PageResult<TaskDO>> getTaskPage(TaskQuery query) {
        PageResult<TaskDO> pageResult = taskService.getTaskPage(query);
        return R.ok(pageResult);
    }

    /**
     * 根据ID查询任务详情
     */
    @Operation(summary = "查询任务详情", description = "根据ID查询任务详情")
    @GetMapping("/{id}")
    public R<TaskDO> getTaskById(@PathVariable String id) {
        TaskDO task = taskService.getById(id);
        return R.ok(task);
    }

    /**
     * 创建任务
     */
    @Operation(summary = "创建任务", description = "创建新的定时任务")
    @PostMapping
    public R<TaskDO> createTask(@RequestBody TaskDO task) {
        TaskDO result = taskService.createTask(task);
        return R.ok(result);
    }

    /**
     * 更新任务
     */
    @Operation(summary = "更新任务", description = "更新定时任务信息")
    @PutMapping("/{id}")
    public R<TaskDO> updateTask(@PathVariable String id, @RequestBody TaskDO task) {
        TaskDO result = taskService.updateTask(id, task);
        return R.ok(result);
    }

    /**
     * 删除任务
     */
    @Operation(summary = "删除任务", description = "删除定时任务")
    @DeleteMapping("/{id}")
    public R<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return R.ok();
    }

    /**
     * 启动任务
     */
    @Operation(summary = "启动任务", description = "启动定时任务")
    @PostMapping("/{id}/start")
    public R<Void> startTask(@PathVariable String id) {
        taskService.startTask(id);
        return R.ok();
    }

    /**
     * 停止任务
     */
    @Operation(summary = "停止任务", description = "停止定时任务")
    @PostMapping("/{id}/stop")
    public R<Void> stopTask(@PathVariable String id) {
        taskService.stopTask(id);
        return R.ok();
    }
}