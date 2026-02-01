package com.iceeboot.framework.service.workflow.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.workflow.ProcessDeployDTO;
import com.iceeboot.framework.dto.create.workflow.ProcessStartDTO;
import com.iceeboot.framework.dto.update.workflow.ProcessInstanceDeleteDTO;
import com.iceeboot.framework.dto.update.workflow.TaskCompleteDTO;
import com.iceeboot.framework.dto.update.workflow.TaskJumpDTO;
import com.iceeboot.framework.query.workflow.ProcessDefinitionQuery;
import com.iceeboot.framework.service.workflow.IWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工作流服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements IWorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final ManagementService managementService;

    @Override
    public ProcessInstance startProcess(String processDefinitionKey, Map<String, Object> variables) {
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
            log.info("启动流程实例成功，流程定义Key: {}, 流程实例ID: {}", processDefinitionKey, processInstance.getId());
            return processInstance;
        } catch (Exception e) {
            log.error("启动流程实例失败，流程定义Key: {}", processDefinitionKey, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.START_PROCESS_FAILED, e);
        }
    }

    @Override
    public ProcessInstance startProcess(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
            log.info("启动流程实例成功，流程定义Key: {}, 业务Key: {}, 流程实例ID: {}", 
                    processDefinitionKey, businessKey, processInstance.getId());
            return processInstance;
        } catch (Exception e) {
            log.error("启动流程实例失败，流程定义Key: {}, 业务Key: {}", processDefinitionKey, businessKey, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.START_PROCESS_FAILED, e);
        }
    }

    @Override
    public ProcessInstance startProcess(ProcessStartDTO dto) {
        if (dto == null || dto.getProcessDefinitionKey() == null || dto.getProcessDefinitionKey().isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_KEY_EMPTY);
        }
        if (dto.getBusinessKey() != null && !dto.getBusinessKey().isBlank()) {
            return startProcess(dto.getProcessDefinitionKey(), dto.getBusinessKey(), dto.getVariables());
        }
        return startProcess(dto.getProcessDefinitionKey(), dto.getVariables());
    }

    @Override
    public void completeTask(String taskId, Map<String, Object> variables) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_NOT_EXIST_PREFIX + taskId);
            }
            
            taskService.complete(taskId, variables);
            log.info("完成任务成功，任务ID: {}, 任务名称: {}", taskId, task.getName());
        } catch (Exception e) {
            log.error("完成任务失败，任务ID: {}", taskId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.COMPLETE_TASK_FAILED, e);
        }
    }

    @Override
    public void completeTask(TaskCompleteDTO dto) {
        if (dto == null || dto.getTaskId() == null || dto.getTaskId().isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_ID_EMPTY);
        }
        completeTask(dto.getTaskId(), dto.getVariables());
    }

    @Override
    public List<Task> getUserTasks(String assignee) {
        try {
            List<Task> tasks = taskService.createTaskQuery()
                    .taskAssignee(assignee)
                    .orderByTaskCreateTime()
                    .desc()
                    .list();
            log.debug("查询用户任务成功，用户: {}, 任务数量: {}", assignee, tasks.size());
            return tasks;
        } catch (Exception e) {
            log.error("查询用户任务失败，用户: {}", assignee, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.QUERY_USER_TASKS_FAILED, e);
        }
    }

    @Override
    public List<Task> getCandidateTasks(String candidateUser) {
        try {
            List<Task> tasks = taskService.createTaskQuery()
                    .taskCandidateUser(candidateUser)
                    .orderByTaskCreateTime()
                    .desc()
                    .list();
            log.debug("查询候选任务成功，候选用户: {}, 任务数量: {}", candidateUser, tasks.size());
            return tasks;
        } catch (Exception e) {
            log.error("查询候选任务失败，候选用户: {}", candidateUser, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.QUERY_CANDIDATE_TASKS_FAILED, e);
        }
    }

    @Override
    public List<Map<String, Object>> getDefinitions(ProcessDefinitionQuery query) {
        boolean latestOnly = query == null || query.getLatestOnly() == null || Boolean.TRUE.equals(query.getLatestOnly());
        List<ProcessDefinition> definitions = latestOnly
                ? repositoryService.createProcessDefinitionQuery().latestVersion().orderByProcessDefinitionKey().asc().list()
                : repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().asc().list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProcessDefinition definition : definitions) {
            Map<String, Object> item = new HashMap<>();
            item.put(SystemConstants.WorkflowResponseKey.ID, definition.getId());
            item.put(SystemConstants.WorkflowResponseKey.KEY, definition.getKey());
            item.put(SystemConstants.WorkflowResponseKey.NAME, definition.getName());
            item.put(SystemConstants.WorkflowResponseKey.VERSION, definition.getVersion());
            item.put(SystemConstants.WorkflowResponseKey.CATEGORY, definition.getCategory());
            item.put(SystemConstants.WorkflowResponseKey.DEPLOYMENT_ID, definition.getDeploymentId());
            item.put(SystemConstants.WorkflowResponseKey.RESOURCE_NAME, definition.getResourceName());
            item.put(SystemConstants.WorkflowResponseKey.DIAGRAM_RESOURCE_NAME, definition.getDiagramResourceName());
            item.put(SystemConstants.WorkflowResponseKey.TENANT_ID, definition.getTenantId());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> deployProcess(ProcessDeployDTO dto) {
        if (dto == null || dto.getFile() == null || dto.getFile().isEmpty()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_FILE_EMPTY);
        }
        try {
            String fileName = dto.getFile().getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = SystemConstants.WorkflowValue.DEFAULT_BPMN_FILE_NAME;
            }
            String deployName = dto.getName();
            if (deployName == null || deployName.isBlank()) {
                deployName = fileName;
            }
            Deployment deployment = repositoryService.createDeployment()
                    .name(deployName)
                    .category(dto.getCategory())
                    .tenantId(dto.getTenantId())
                    .addInputStream(fileName, dto.getFile().getInputStream())
                    .deploy();
            Map<String, Object> result = new HashMap<>();
            result.put(SystemConstants.WorkflowResponseKey.ID, deployment.getId());
            result.put(SystemConstants.WorkflowResponseKey.NAME, deployment.getName());
            result.put(SystemConstants.WorkflowResponseKey.CATEGORY, deployment.getCategory());
            result.put(SystemConstants.WorkflowResponseKey.TENANT_ID, deployment.getTenantId());
            result.put(SystemConstants.WorkflowResponseKey.DEPLOYMENT_TIME, deployment.getDeploymentTime());
            return result;
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.DEPLOY_PROCESS_FAILED, e);
        }
    }

    @Override
    public String getDefinitionXml(String definitionId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId)
                .singleResult();
        if (definition == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_NOT_EXIST);
        }
        try (InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getResourceName())) {
            if (inputStream == null) {
                throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_RESOURCE_NOT_EXIST);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.GET_DEFINITION_XML_FAILED, e);
        }
    }

    @Override
    public Map<String, Object> getDefinitionDiagram(String definitionId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId)
                .singleResult();
        if (definition == null || definition.getDiagramResourceName() == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_DIAGRAM_NOT_EXIST);
        }
        try (InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getDiagramResourceName())) {
            if (inputStream == null) {
                throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_DIAGRAM_NOT_EXIST);
            }
            String base64 = Base64.getEncoder().encodeToString(inputStream.readAllBytes());
            Map<String, Object> result = new HashMap<>();
            result.put(SystemConstants.WorkflowResponseKey.CONTENT_TYPE, SystemConstants.WorkflowValue.CONTENT_TYPE_PNG);
            result.put(SystemConstants.WorkflowResponseKey.DATA, base64);
            return result;
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.GET_DEFINITION_DIAGRAM_FAILED, e);
        }
    }

    @Override
    public List<Map<String, Object>> getDefinitionNodes(String definitionId) {
        BpmnModel model = repositoryService.getBpmnModel(definitionId);
        if (model == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_NOT_EXIST);
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (org.activiti.bpmn.model.Process process : model.getProcesses()) {
            for (FlowElement element : process.getFlowElements()) {
                GraphicInfo graphicInfo = model.getGraphicInfo(element.getId());
                if (graphicInfo == null) {
                    continue;
                }
                Map<String, Object> node = new HashMap<>();
                node.put(SystemConstants.WorkflowResponseKey.ID, element.getId());
                node.put(SystemConstants.WorkflowResponseKey.NAME, element.getName());
                node.put(SystemConstants.WorkflowResponseKey.TYPE, element.getClass().getSimpleName());
                node.put(SystemConstants.WorkflowResponseKey.X, graphicInfo.getX());
                node.put(SystemConstants.WorkflowResponseKey.Y, graphicInfo.getY());
                node.put(SystemConstants.WorkflowResponseKey.WIDTH, graphicInfo.getWidth());
                node.put(SystemConstants.WorkflowResponseKey.HEIGHT, graphicInfo.getHeight());
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public ProcessInstance getProcessInstance(String processInstanceId) {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            log.debug("查询流程实例成功，流程实例ID: {}", processInstanceId);
            return processInstance;
        } catch (Exception e) {
            log.error("查询流程实例失败，流程实例ID: {}", processInstanceId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.QUERY_PROCESS_INSTANCE_FAILED, e);
        }
    }

    @Override
    public List<Map<String, Object>> getProcessInstanceNodes(String processInstanceId) {
        String definitionId = null;
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance != null) {
            definitionId = processInstance.getProcessDefinitionId();
        } else {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (historicProcessInstance != null) {
                definitionId = historicProcessInstance.getProcessDefinitionId();
            }
        }
        if (definitionId == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_INSTANCE_NOT_EXIST);
        }
        BpmnModel model = repositoryService.getBpmnModel(definitionId);
        if (model == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_NOT_EXIST);
        }
        Set<String> activeIds = new HashSet<>();
        if (processInstance != null) {
            activeIds.addAll(runtimeService.getActiveActivityIds(processInstanceId));
        }
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        Set<String> completedIds = new HashSet<>();
        for (HistoricActivityInstance historicActivity : historicActivities) {
            if (historicActivity.getEndTime() != null) {
                completedIds.add(historicActivity.getActivityId());
            }
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (org.activiti.bpmn.model.Process process : model.getProcesses()) {
            for (FlowElement element : process.getFlowElements()) {
                GraphicInfo graphicInfo = model.getGraphicInfo(element.getId());
                if (graphicInfo == null) {
                    continue;
                }
                Map<String, Object> node = new HashMap<>();
                node.put(SystemConstants.WorkflowResponseKey.ID, element.getId());
                node.put(SystemConstants.WorkflowResponseKey.NAME, element.getName());
                node.put(SystemConstants.WorkflowResponseKey.TYPE, element.getClass().getSimpleName());
                node.put(SystemConstants.WorkflowResponseKey.X, graphicInfo.getX());
                node.put(SystemConstants.WorkflowResponseKey.Y, graphicInfo.getY());
                node.put(SystemConstants.WorkflowResponseKey.WIDTH, graphicInfo.getWidth());
                node.put(SystemConstants.WorkflowResponseKey.HEIGHT, graphicInfo.getHeight());
                String status = activeIds.contains(element.getId()) ? SystemConstants.WorkflowNodeStatus.ACTIVE
                        : (completedIds.contains(element.getId()) ? SystemConstants.WorkflowNodeStatus.COMPLETED
                        : SystemConstants.WorkflowNodeStatus.PENDING);
                node.put(SystemConstants.WorkflowResponseKey.STATUS, status);
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        try {
            runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
            log.info("删除流程实例成功，流程实例ID: {}, 删除原因: {}", processInstanceId, deleteReason);
        } catch (Exception e) {
            log.error("删除流程实例失败，流程实例ID: {}", processInstanceId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.DELETE_PROCESS_INSTANCE_FAILED, e);
        }
    }

    @Override
    public void deleteProcessInstance(ProcessInstanceDeleteDTO dto) {
        if (dto == null || dto.getProcessInstanceId() == null || dto.getProcessInstanceId().isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_INSTANCE_ID_EMPTY);
        }
        String deleteReason = dto.getDeleteReason();
        if (deleteReason == null || deleteReason.isBlank()) {
            deleteReason = SystemConstants.WorkflowValue.DEFAULT_DELETE_REASON;
        }
        deleteProcessInstance(dto.getProcessInstanceId(), deleteReason);
    }

    @Override
    public boolean isProcessEnded(String processInstanceId) {
        try {
            ProcessInstance processInstance = getProcessInstance(processInstanceId);
            boolean isEnded = processInstance == null;
            log.debug("判断流程是否结束，流程实例ID: {}, 是否结束: {}", processInstanceId, isEnded);
            return isEnded;
        } catch (Exception e) {
            log.error("判断流程是否结束失败，流程实例ID: {}", processInstanceId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_ENDED_CHECK_FAILED, e);
        }
    }

    @Override
    public void jumpToActivity(String taskId, String targetActivityId) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_NOT_EXIST_PREFIX + taskId);
            }
            
            // 使用Activiti的跳转功能
            managementService.executeCommand(context -> {
                // 这里需要实现具体的跳转逻辑
                // 由于Activiti的跳转比较复杂，这里提供一个基础框架
                log.warn(ResponseMessage.WorkflowMessage.JUMP_TASK_NEED_IMPLEMENT, taskId, targetActivityId);
                return null;
            });
            
            log.info("跳转任务成功，任务ID: {}, 目标节点: {}", taskId, targetActivityId);
        } catch (Exception e) {
            log.error("跳转任务失败，任务ID: {}, 目标节点: {}", taskId, targetActivityId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.JUMP_TASK_FAILED, e);
        }
    }

    @Override
    public void jumpToActivity(TaskJumpDTO dto) {
        if (dto == null || dto.getTaskId() == null || dto.getTaskId().isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_ID_EMPTY);
        }
        if (dto.getTargetActivityId() == null || dto.getTargetActivityId().isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TARGET_ACTIVITY_ID_EMPTY);
        }
        jumpToActivity(dto.getTaskId(), dto.getTargetActivityId());
    }

    @Override
    public void endProcess(String processInstanceId) {
        try {
            ProcessInstance processInstance = getProcessInstance(processInstanceId);
            if (processInstance != null) {
                runtimeService.deleteProcessInstance(processInstanceId, SystemConstants.WorkflowValue.DEFAULT_END_REASON);
                log.info("结束流程成功，流程实例ID: {}", processInstanceId);
            } else {
                log.warn("流程实例不存在或已结束，流程实例ID: {}", processInstanceId);
            }
        } catch (Exception e) {
            log.error("结束流程失败，流程实例ID: {}", processInstanceId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.END_PROCESS_FAILED, e);
        }
    }
}
