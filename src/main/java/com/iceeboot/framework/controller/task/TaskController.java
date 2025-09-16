package com.iceeboot.framework.controller.task;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.task.TaskDTO;
import com.iceeboot.framework.entity.task.TaskDO;
import com.iceeboot.framework.query.system.TaskQuery;
import com.iceeboot.framework.service.task.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-12
 */
@Tag(name = "定时任务管理", description = "定时任务管理相关接口")
@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;

    @Operation(summary = "分页查询任务列表", description = "根据条件分页查询任务列表")
    @GetMapping()
    @RequirePermission(dynamic = true)
    public R<PageResult<TaskDO>> getTaskPage(TaskQuery query) {
        PageResult<TaskDO> pageResult = taskService.getTaskPage(query);
        return R.ok(pageResult);
    }


    @Operation(summary = "查询任务详情", description = "根据ID查询任务详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<TaskDO> getTaskById(@PathVariable("id") String id) {
        TaskDO task = taskService.getById(id);
        return R.ok(task);
    }


    @Operation(summary = "创建任务", description = "创建新的定时任务")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> createTask(@Validated @RequestBody TaskDTO taskDTO) {
        boolean success = taskService.createTask(taskDTO);
        return success ? R.ok(ResponseMessage.TASK_CREATE_SUCCESS) : R.fail(ResponseMessage.TASK_CREATE_FAIL);
    }


    @Operation(summary = "更新任务", description = "更新定时任务信息")
    @PutMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<TaskDO> updateTask(@PathVariable("id") Long id, @RequestBody TaskDO task) {
        TaskDO result = taskService.updateTask(id, task);
        return R.ok(result);
    }


    @Operation(summary = "删除任务", description = "单个删除或批量删除定时任务")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteTasks(@RequestBody List<Long> ids) {
        boolean success = taskService.deleteTasks(ids);
        return success ? R.ok(ResponseMessage.TASK_DELETE_SUCCESS) : R.fail(ResponseMessage.TASK_DELETE_FAIL);
    }


    @Operation(summary = "启动任务", description = "启动定时任务")
    @PostMapping("/{id}/start")
    @RequirePermission(dynamic = true)
    public R<Void> startTask(@PathVariable("id") String id) {
        taskService.startTask(id);
        return R.ok();
    }


    @Operation(summary = "停止任务", description = "停止定时任务")
    @PostMapping("/{id}/stop")
    @RequirePermission(dynamic = true)
    public R<Void> stopTask(@PathVariable("id") String id) {
        taskService.stopTask(id);
        return R.ok();
    }
}