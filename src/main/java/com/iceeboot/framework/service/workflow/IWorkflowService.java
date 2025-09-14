package com.iceeboot.framework.service.workflow;

import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import java.util.List;
import java.util.Map;

/**
 * 工作流服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
public interface IWorkflowService {

    /**
     * 启动流程实例
     * 
     * @param processDefinitionKey 流程定义Key
     * @param variables 流程变量
     * @return 流程实例
     */
    ProcessInstance startProcess(String processDefinitionKey, Map<String, Object> variables);

    /**
     * 启动流程实例
     * 
     * @param processDefinitionKey 流程定义Key
     * @param businessKey 业务Key
     * @param variables 流程变量
     * @return 流程实例
     */
    ProcessInstance startProcess(String processDefinitionKey, String businessKey, Map<String, Object> variables);

    /**
     * 完成任务
     * 
     * @param taskId 任务ID
     * @param variables 任务变量
     */
    void completeTask(String taskId, Map<String, Object> variables);

    /**
     * 获取用户任务列表
     * 
     * @param assignee 任务分配人
     * @return 任务列表
     */
    List<Task> getUserTasks(String assignee);

    /**
     * 获取候选任务列表
     * 
     * @param candidateUser 候选用户
     * @return 任务列表
     */
    List<Task> getCandidateTasks(String candidateUser);

    /**
     * 获取流程实例
     * 
     * @param processInstanceId 流程实例ID
     * @return 流程实例
     */
    ProcessInstance getProcessInstance(String processInstanceId);

    /**
     * 删除流程实例
     * 
     * @param processInstanceId 流程实例ID
     * @param deleteReason 删除原因
     */
    void deleteProcessInstance(String processInstanceId, String deleteReason);

    /**
     * 判断流程是否结束
     * 
     * @param processInstanceId 流程实例ID
     * @return 是否结束
     */
    boolean isProcessEnded(String processInstanceId);

    /**
     * 跳转到指定节点
     * 
     * @param taskId 当前任务ID
     * @param targetActivityId 目标节点ID
     */
    void jumpToActivity(String taskId, String targetActivityId);

    /**
     * 结束流程
     * 
     * @param processInstanceId 流程实例ID
     */
    void endProcess(String processInstanceId);
}