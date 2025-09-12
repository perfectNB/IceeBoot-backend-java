package com.zboot.framework.controller.workflow;

import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.core.domain.R;
import com.zboot.framework.service.workflow.IWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流控制器
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Tag(name = "工作流管理", description = "工作流管理相关接口")
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final IWorkflowService workflowService;

    /**
     * 启动流程实例
     */
    @Operation(summary = "启动流程实例", description = "根据流程定义Key启动流程实例")
    @PostMapping("/start")
    @RequirePermission(dynamic = true)
    public R<ProcessInstance> startProcess(
            @RequestParam String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestBody(required = false) Map<String, Object> variables) {
        
        ProcessInstance processInstance;
        if (businessKey != null) {
            processInstance = workflowService.startProcess(processDefinitionKey, businessKey, variables);
        } else {
            processInstance = workflowService.startProcess(processDefinitionKey, variables);
        }
        
        return R.ok(processInstance);
    }

    /**
     * 完成任务
     */
    @Operation(summary = "完成任务", description = "完成指定的任务")
    @PostMapping("/task/{taskId}/complete")
    @RequirePermission(dynamic = true)
    public R<Void> completeTask(
            @PathVariable("taskId") String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {
        
        workflowService.completeTask(taskId, variables);
        return R.ok();
    }

    /**
     * 获取用户任务列表
     */
    @Operation(summary = "获取用户任务列表", description = "获取指定用户的任务列表")
    @GetMapping("/tasks/user/{assignee}")
    @RequirePermission(dynamic = true)
    public R<List<Task>> getUserTasks(@PathVariable("assignee") String assignee) {
        List<Task> tasks = workflowService.getUserTasks(assignee);
        return R.ok(tasks);
    }

    /**
     * 获取候选任务列表
     */
    @Operation(summary = "获取候选任务列表", description = "获取指定候选用户的任务列表")
    @GetMapping("/tasks/candidate/{candidateUser}")
    @RequirePermission(dynamic = true)
    public R<List<Task>> getCandidateTasks(@PathVariable("candidateUser") String candidateUser) {
        List<Task> tasks = workflowService.getCandidateTasks(candidateUser);
        return R.ok(tasks);
    }

    /**
     * 获取流程实例
     */
    @Operation(summary = "获取流程实例", description = "根据流程实例ID获取流程实例")
    @GetMapping("/process/{processInstanceId}")
    @RequirePermission(dynamic = true)
    public R<ProcessInstance> getProcessInstance(@PathVariable("processInstanceId") String processInstanceId) {
        ProcessInstance processInstance = workflowService.getProcessInstance(processInstanceId);
        return R.ok(processInstance);
    }

    /**
     * 删除流程实例
     */
    @Operation(summary = "删除流程实例", description = "删除指定的流程实例")
    @DeleteMapping("/process/{processInstanceId}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteProcessInstance(
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam(defaultValue = "手动删除") String deleteReason) {
        
        workflowService.deleteProcessInstance(processInstanceId, deleteReason);
        return R.ok();
    }

    /**
     * 判断流程是否结束
     */
    @Operation(summary = "判断流程是否结束", description = "判断指定流程实例是否已结束")
    @GetMapping("/process/{processInstanceId}/ended")
    @RequirePermission(dynamic = true)
    public R<Boolean> isProcessEnded(@PathVariable("processInstanceId") String processInstanceId) {
        boolean isEnded = workflowService.isProcessEnded(processInstanceId);
        return R.ok(isEnded);
    }

    /**
     * 跳转到指定节点
     */
    @Operation(summary = "跳转到指定节点", description = "将任务跳转到指定的活动节点")
    @PostMapping("/task/{taskId}/jump")
    @RequirePermission(dynamic = true)
    public R<Void> jumpToActivity(
            @PathVariable("taskId") String taskId,
            @RequestParam String targetActivityId) {
        
        workflowService.jumpToActivity(taskId, targetActivityId);
        return R.ok();
    }

    /**
     * 结束流程
     */
    @Operation(summary = "结束流程", description = "强制结束指定的流程实例")
    @PostMapping("/process/{processInstanceId}/end")
    @RequirePermission(dynamic = true)
    public R<Void> endProcess(@PathVariable("processInstanceId") String processInstanceId) {
        workflowService.endProcess(processInstanceId);
        return R.ok();
    }
}