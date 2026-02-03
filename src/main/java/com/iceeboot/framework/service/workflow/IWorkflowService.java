package com.iceeboot.framework.service.workflow;

import com.iceeboot.framework.dto.create.workflow.ProcessDeployDTO;
import com.iceeboot.framework.dto.create.workflow.ProcessStartDTO;
import com.iceeboot.framework.dto.update.workflow.ProcessInstanceDeleteDTO;
import com.iceeboot.framework.dto.update.workflow.TaskCompleteDTO;
import com.iceeboot.framework.query.workflow.ProcessDefinitionQuery;
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

    ProcessInstance startProcess(ProcessStartDTO dto);

    /**
     * 完成任务
     * 
     * @param taskId 任务ID
     * @param variables 任务变量
     */
    void completeTask(String taskId, Map<String, Object> variables);

    void completeTask(TaskCompleteDTO dto);

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

    void claimTask(String taskId, String userId);

    void unclaimTask(String taskId);

    List<Task> getMyTasks();

    List<Map<String, Object>> getDefinitions(ProcessDefinitionQuery query);

    Map<String, Object> deployProcess(ProcessDeployDTO dto);

    Map<String, Object> updateProcessDefinition(String definitionId, ProcessDeployDTO dto);

    void deleteProcessDefinition(String definitionId, boolean cascade);

    String getDefinitionXml(String definitionId);

    Map<String, Object> getDefinitionDiagram(String definitionId);

    List<Map<String, Object>> getDefinitionNodes(String definitionId);

    List<Map<String, Object>> getProcessInstances(String definitionKey, String businessKey, Boolean includeEnded);

    /**
     * 获取流程实例
     * 
     * @param processInstanceId 流程实例ID
     * @return 流程实例
     */
    ProcessInstance getProcessInstance(String processInstanceId);

    List<Map<String, Object>> getProcessInstanceNodes(String processInstanceId);

    Map<String, Object> getProcessInstanceDiagram(String processInstanceId);

    List<Map<String, Object>> getProcessInstanceHistory(String processInstanceId);

    /**
     * 删除流程实例
     * 
     * @param processInstanceId 流程实例ID
     * @param deleteReason 删除原因
     */
    void deleteProcessInstance(String processInstanceId, String deleteReason);

    void deleteProcessInstance(ProcessInstanceDeleteDTO dto);

    /**
     * 判断流程是否结束
     * 
     * @param processInstanceId 流程实例ID
     * @return 是否结束
     */
    boolean isProcessEnded(String processInstanceId);

    /**
     * 结束流程
     * 
     * @param processInstanceId 流程实例ID
     */
    void endProcess(String processInstanceId);
}
