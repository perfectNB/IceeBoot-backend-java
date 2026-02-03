package com.iceeboot.framework.controller.workflow;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.workflow.ProcessDeployDTO;
import com.iceeboot.framework.dto.create.workflow.ProcessStartDTO;
import com.iceeboot.framework.dto.update.workflow.ProcessInstanceDeleteDTO;
import com.iceeboot.framework.dto.update.workflow.TaskCompleteDTO;
import com.iceeboot.framework.query.workflow.ProcessDefinitionQuery;
import com.iceeboot.framework.service.workflow.IWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/v1/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final IWorkflowService workflowService;

    /**
     * 启动流程实例
     */
    @Operation(summary = "启动流程实例", description = "根据流程定义Key启动流程实例")
    @PostMapping("/start")
    @RequirePermission(dynamic = true)
    public R<ProcessInstance> startProcess(@Validated @RequestBody ProcessStartDTO dto) {
        ProcessInstance processInstance = workflowService.startProcess(dto);
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
            @RequestBody(required = false) TaskCompleteDTO dto) {
        TaskCompleteDTO request = dto == null ? new TaskCompleteDTO() : dto;
        request.setTaskId(taskId);
        workflowService.completeTask(request);
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

    @Operation(summary = "获取我的任务列表", description = "获取当前登录用户的任务列表")
    @GetMapping("/tasks/my")
    @RequirePermission(dynamic = true)
    public R<List<Task>> getMyTasks() {
        List<Task> tasks = workflowService.getMyTasks();
        return R.ok(tasks);
    }

    @Operation(summary = "认领任务", description = "认领指定任务")
    @PostMapping("/task/{taskId}/claim")
    @RequirePermission(dynamic = true)
    public R<Void> claimTask(@PathVariable("taskId") String taskId, @RequestParam("userId") String userId) {
        workflowService.claimTask(taskId, userId);
        return R.ok();
    }

    @Operation(summary = "取消认领任务", description = "取消认领指定任务")
    @PostMapping("/task/{taskId}/unclaim")
    @RequirePermission(dynamic = true)
    public R<Void> unclaimTask(@PathVariable("taskId") String taskId) {
        workflowService.unclaimTask(taskId);
        return R.ok();
    }

    @Operation(summary = "流程定义列表", description = "获取流程定义列表")
    @GetMapping("/definitions")
    @RequirePermission(dynamic = true)
    public R<List<Map<String, Object>>> getDefinitions(ProcessDefinitionQuery query) {
        List<Map<String, Object>> result = workflowService.getDefinitions(query);
        return R.ok(result);
    }

    @Operation(summary = "部署流程定义", description = "上传BPMN文件部署流程")
    @PostMapping("/deploy")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> deployProcess(@ModelAttribute ProcessDeployDTO dto) {
        Map<String, Object> result = workflowService.deployProcess(dto);
        return R.ok(result);
    }

    @Operation(summary = "更新流程定义", description = "上传BPMN文件更新流程定义")
    @PutMapping("/definitions/{definitionId}/redeploy")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> updateProcessDefinition(
            @PathVariable("definitionId") String definitionId,
            @ModelAttribute ProcessDeployDTO dto) {
        Map<String, Object> result = workflowService.updateProcessDefinition(definitionId, dto);
        return R.ok(result);
    }

    @Operation(summary = "删除流程定义", description = "删除指定流程定义")
    @DeleteMapping("/definitions/{definitionId}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteProcessDefinition(
            @PathVariable("definitionId") String definitionId,
            @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade) {
        workflowService.deleteProcessDefinition(definitionId, cascade);
        return R.ok();
    }

    @Operation(summary = "获取流程定义XML", description = "获取流程定义的BPMN XML")
    @GetMapping("/definitions/{definitionId}/xml")
    @RequirePermission(dynamic = true)
    public R<String> getDefinitionXml(@PathVariable("definitionId") String definitionId) {
        String xml = workflowService.getDefinitionXml(definitionId);
        return R.ok(xml);
    }

    @Operation(summary = "获取流程定义图", description = "获取流程定义的图片Base64")
    @GetMapping("/definitions/{definitionId}/diagram")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getDefinitionDiagram(@PathVariable("definitionId") String definitionId) {
        Map<String, Object> result = workflowService.getDefinitionDiagram(definitionId);
        return R.ok(result);
    }

    @Operation(summary = "获取流程定义节点", description = "获取流程定义中所有节点与坐标")
    @GetMapping("/definitions/{definitionId}/nodes")
    @RequirePermission(dynamic = true)
    public R<List<Map<String, Object>>> getDefinitionNodes(@PathVariable("definitionId") String definitionId) {
        List<Map<String, Object>> nodes = workflowService.getDefinitionNodes(definitionId);
        return R.ok(nodes);
    }

    @Operation(summary = "流程实例列表", description = "根据条件获取流程实例列表")
    @GetMapping("/process")
    @RequirePermission(dynamic = true)
    public R<List<Map<String, Object>>> getProcessInstances(
            @RequestParam(value = "definitionKey", required = false) String definitionKey,
            @RequestParam(value = "businessKey", required = false) String businessKey,
            @RequestParam(value = "includeEnded", required = false) Boolean includeEnded) {
        List<Map<String, Object>> result = workflowService.getProcessInstances(definitionKey, businessKey, includeEnded);
        return R.ok(result);
    }

    @Operation(summary = "获取流程实例", description = "根据流程实例ID获取流程实例")
    @GetMapping("/process/{processInstanceId}")
    @RequirePermission(dynamic = true)
    public R<ProcessInstance> getProcessInstance(@PathVariable("processInstanceId") String processInstanceId) {
        ProcessInstance processInstance = workflowService.getProcessInstance(processInstanceId);
        return R.ok(processInstance);
    }

    @Operation(summary = "获取流程实例节点", description = "获取流程实例节点状态与坐标")
    @GetMapping("/process/{processInstanceId}/nodes")
    @RequirePermission(dynamic = true)
    public R<List<Map<String, Object>>> getProcessInstanceNodes(@PathVariable("processInstanceId") String processInstanceId) {
        List<Map<String, Object>> nodes = workflowService.getProcessInstanceNodes(processInstanceId);
        return R.ok(nodes);
    }

    @Operation(summary = "获取流程实例图", description = "获取流程实例的图片Base64（包含高亮节点）")
    @GetMapping("/process/{processInstanceId}/diagram")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getProcessInstanceDiagram(@PathVariable("processInstanceId") String processInstanceId) {
        Map<String, Object> result = workflowService.getProcessInstanceDiagram(processInstanceId);
        return R.ok(result);
    }

    @Operation(summary = "获取流程实例历史", description = "获取流程实例审批历史与意见")
    @GetMapping("/process/{processInstanceId}/history")
    @RequirePermission(dynamic = true)
    public R<List<Map<String, Object>>> getProcessInstanceHistory(@PathVariable("processInstanceId") String processInstanceId) {
        List<Map<String, Object>> history = workflowService.getProcessInstanceHistory(processInstanceId);
        return R.ok(history);
    }

    /**
     * 删除流程实例
     */
    @Operation(summary = "删除流程实例", description = "删除指定的流程实例")
    @DeleteMapping("/process/{processInstanceId}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteProcessInstance(
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestBody(required = false) ProcessInstanceDeleteDTO dto) {
        ProcessInstanceDeleteDTO request = dto == null ? new ProcessInstanceDeleteDTO() : dto;
        request.setProcessInstanceId(processInstanceId);
        workflowService.deleteProcessInstance(request);
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
