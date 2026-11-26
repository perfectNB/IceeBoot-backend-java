package com.iceeboot.framework.service.workflow.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.workflow.ProcessDeployDTO;
import com.iceeboot.framework.dto.create.workflow.ProcessStartDTO;
import com.iceeboot.framework.dto.update.workflow.ProcessInstanceDeleteDTO;
import com.iceeboot.framework.dto.update.workflow.TaskCompleteDTO;
import com.iceeboot.framework.query.workflow.ProcessDefinitionQuery;
import com.iceeboot.framework.service.workflow.IWorkflowService;
import com.iceeboot.framework.service.system.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.identity.Authentication;
import com.iceeboot.framework.vo.workflow.ProcessInstanceVO;
import com.iceeboot.common.utils.TokenUtils;
import org.springframework.transaction.annotation.Transactional;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.Comment;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
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
import java.util.Comparator;

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
    private final ISysUserService userService;

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
    @Transactional
    public ProcessInstance startProcess(ProcessStartDTO dto) {
        if (dto == null || dto.getProcessDefinitionKey() == null || dto.getProcessDefinitionKey().isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_KEY_EMPTY);
        }
        String previous = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId(TokenUtils.getCurrentUserIdAsString());
        try {
            ProcessInstance processInstance;
            if (dto.getBusinessKey() != null && !dto.getBusinessKey().isBlank()) {
                processInstance = startProcess(dto.getProcessDefinitionKey(), dto.getBusinessKey(), dto.getVariables());
            } else {
                processInstance = startProcess(dto.getProcessDefinitionKey(), dto.getVariables());
            }
            if (dto.getProcessInstanceName() != null && !dto.getProcessInstanceName().isBlank()) {
                runtimeService.setProcessInstanceName(processInstance.getId(), dto.getProcessInstanceName());
            }
            return processInstance;
        } finally {
            Authentication.setAuthenticatedUserId(previous);
        }
    }

    @Override
    @Transactional
    public void completeTask(String taskId, Map<String, Object> variables) {
        completeTask(taskId, variables, null);
    }

    @Override
    @Transactional
    public void completeTask(TaskCompleteDTO dto) {
        if (dto == null || dto.getTaskId() == null || dto.getTaskId().isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_ID_EMPTY);
        }
        completeTask(dto.getTaskId(), dto.getVariables(), dto.getComment());
    }

    @Override
    public List<Task> getUserTasks(String assignee) {
        Long actor = requireActor();
        String username = actorName(actor);
        requireOwnTaskList(assignee, actor, username);
        try {
            Set<String> assigneeCandidates = new HashSet<>();
            assigneeCandidates.add(actor.toString());
            assigneeCandidates.add(username);
            List<String> roleCodes = userService.getUserRoles(actor);
            Map<String, Task> matches = new HashMap<>();
            for (String candidate : assigneeCandidates) {
                addTasks(matches, taskService.createTaskQuery().taskAssignee(candidate).list());
            }
            if (roleCodes != null && !roleCodes.isEmpty()) {
                addTasks(matches, taskService.createTaskQuery().taskCandidateGroupIn(roleCodes).list());
            }
            List<Task> tasks = matches.values().stream().sorted(Comparator.comparing(Task::getCreateTime).reversed()).toList();
            log.debug("查询用户任务成功，用户: {}, 任务数量: {}", assignee, tasks.size());
            return tasks;
        } catch (Exception e) {
            log.error("查询用户任务失败，用户: {}", assignee, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.QUERY_USER_TASKS_FAILED, e);
        }
    }

    @Override
    public List<Task> getCandidateTasks(String candidateUser) {
        Long actor = requireActor();
        requireOwnTaskList(candidateUser, actor, actorName(actor));
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
    @Transactional
    public void claimTask(String taskId, String userId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_ID_EMPTY);
        }
        Long actor = requireActor();
        String username = actorName(actor);
        if (userId != null && !userId.equals(actor.toString()) && !userId.equals(username)) {
            throw new IceeBootException("只能为当前用户认领任务");
        }
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_NOT_EXIST_PREFIX + taskId);
            }
            if (task.getAssignee() != null) {
                requireAssignee(task);
                return;
            }
            List<String> roles = userService.getUserRoles(actor);
            boolean eligible = taskService.getIdentityLinksForTask(taskId).stream().anyMatch(link ->
                    "candidate".equals(link.getType()) && (actor.toString().equals(link.getUserId())
                    || (username != null && username.equals(link.getUserId()))
                    || (link.getGroupId() != null && roles != null && roles.contains(link.getGroupId()))));
            if (!eligible) throw new IceeBootException("当前用户不是任务候选人");
            taskService.claim(taskId, actor.toString());
            log.info("认领任务成功，任务ID: {}, 认领人: {}", taskId, actor);
        } catch (Exception e) {
            log.error("认领任务失败，任务ID: {}, 认领人: {}", taskId, userId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_CLAIM_FAILED, e);
        }
    }

    @Override
    @Transactional
    public void unclaimTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_ID_EMPTY);
        }
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_NOT_EXIST_PREFIX + taskId);
            }
            requireAssignee(task);
            taskService.unclaim(taskId);
            log.info("取消认领任务成功，任务ID: {}", taskId);
        } catch (Exception e) {
            log.error("取消认领任务失败，任务ID: {}", taskId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_UNCLAIM_FAILED, e);
        }
    }

    @Override
    public List<Task> getMyTasks() {
        Long userId = com.iceeboot.common.utils.TokenUtils.getCurrentUserId();
        if (userId == null) {
            throw new IceeBootException(ResponseMessage.Common.UNAUTHORIZED);
        }
        var user = userService.getById(userId);
        if (user == null) {
            throw new IceeBootException(ResponseMessage.Auth.USER_NOT_EXIST);
        }
        String userIdString = String.valueOf(userId);
        String username = user.getUsername();
        List<String> roleCodes = userService.getUserRoles(userId);
        Map<String, Task> taskMap = new HashMap<>();
        addTasks(taskMap, taskService.createTaskQuery().taskAssignee(userIdString).list());
        if (username != null && !username.isBlank()) {
            addTasks(taskMap, taskService.createTaskQuery().taskAssignee(username).list());
        }
        addTasks(taskMap, taskService.createTaskQuery().taskCandidateUser(userIdString).list());
        if (username != null && !username.isBlank()) {
            addTasks(taskMap, taskService.createTaskQuery().taskCandidateUser(username).list());
        }
        if (roleCodes != null && !roleCodes.isEmpty()) {
            addTasks(taskMap, taskService.createTaskQuery().taskCandidateGroupIn(roleCodes).list());
        }
        return taskMap.values().stream()
                .sorted(Comparator.comparing(Task::getCreateTime).reversed())
                .toList();
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
    public Map<String, Object> updateProcessDefinition(String definitionId, ProcessDeployDTO dto) {
        if (definitionId == null || definitionId.isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_ID_EMPTY);
        }
        if (dto == null || dto.getFile() == null || dto.getFile().isEmpty()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_FILE_EMPTY);
        }
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId)
                .singleResult();
        if (definition == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_NOT_EXIST);
        }
        try {
            String fileName = dto.getFile().getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = SystemConstants.WorkflowValue.DEFAULT_BPMN_FILE_NAME;
            }
            String deployName = dto.getName();
            if (deployName == null || deployName.isBlank()) {
                deployName = definition.getName();
            }
            String category = dto.getCategory();
            String tenantId = dto.getTenantId();
            Deployment deployment = repositoryService.createDeployment()
                    .name(deployName)
                    .category(category != null ? category : definition.getCategory())
                    .tenantId(tenantId != null ? tenantId : definition.getTenantId())
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
            throw new IceeBootException(ResponseMessage.WorkflowMessage.UPDATE_PROCESS_DEFINITION_FAILED, e);
        }
    }

    @Override
    public void deleteProcessDefinition(String definitionId, boolean cascade) {
        if (definitionId == null || definitionId.isBlank()) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_ID_EMPTY);
        }
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId)
                .singleResult();
        if (definition == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_NOT_EXIST);
        }
        try {
            repositoryService.deleteDeployment(definition.getDeploymentId(), cascade);
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.DELETE_PROCESS_DEFINITION_FAILED, e);
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
        if (definition == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_NOT_EXIST);
        }
        String diagramResourceName = definition.getDiagramResourceName();
        if (diagramResourceName != null) {
            try (InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), diagramResourceName)) {
                if (inputStream != null) {
                    return buildContentResult(inputStream, SystemConstants.WorkflowValue.CONTENT_TYPE_PNG);
                }
            } catch (Exception e) {
                log.warn("读取流程定义图片失败，流程定义ID: {}", definitionId, e);
            }
        }
        try {
            return generateDiagramResult(definitionId);
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.GET_DEFINITION_DIAGRAM_FAILED, e);
        }
    }

    private Map<String, Object> generateDiagramResult(String definitionId) throws Exception {
        BpmnModel model = repositoryService.getBpmnModel(definitionId);
        if (model == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_NOT_EXIST);
        }
        ProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        InputStream diagramStream = diagramGenerator.generateDiagram(
                model,
                List.of(),
                List.of(),
                "Arial",
                "Arial",
                "Arial",
                false,
                null
        );
        if (diagramStream == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_DIAGRAM_NOT_EXIST);
        }
        try (InputStream inputStream = diagramStream) {
            return buildContentResult(inputStream, SystemConstants.WorkflowValue.CONTENT_TYPE_PNG);
        }
    }

    private Map<String, Object> buildContentResult(InputStream inputStream, String contentType) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        Map<String, Object> result = new HashMap<>();
        result.put(SystemConstants.WorkflowResponseKey.CONTENT_TYPE, contentType);
        result.put(SystemConstants.WorkflowResponseKey.DATA, base64);
        return result;
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
    public List<Map<String, Object>> getProcessInstances(String definitionKey, String businessKey, Boolean includeEnded) {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            var runtimeQuery = runtimeService.createProcessInstanceQuery();
            if (definitionKey != null && !definitionKey.isBlank()) {
                runtimeQuery.processDefinitionKey(definitionKey);
            }
            if (businessKey != null && !businessKey.isBlank()) {
                runtimeQuery.processInstanceBusinessKey(businessKey);
            }
            List<ProcessInstance> runningInstances = runtimeQuery.orderByProcessInstanceId().desc().list();
            for (ProcessInstance instance : runningInstances) {
                Map<String, Object> item = new HashMap<>();
                item.put(SystemConstants.WorkflowResponseKey.ID, instance.getId());
                item.put(SystemConstants.WorkflowResponseKey.PROCESS_DEFINITION_ID, instance.getProcessDefinitionId());
                item.put(SystemConstants.WorkflowResponseKey.KEY, instance.getProcessDefinitionKey());
                item.put(SystemConstants.WorkflowResponseKey.NAME, instance.getName());
                item.put(SystemConstants.WorkflowResponseKey.BUSINESS_KEY, instance.getBusinessKey());
                item.put(SystemConstants.WorkflowResponseKey.START_TIME, instance.getStartTime());
                item.put(SystemConstants.WorkflowResponseKey.STATUS, SystemConstants.WorkflowNodeStatus.ACTIVE);
                result.add(item);
            }
            if (includeEnded != null && includeEnded) {
                var historyQuery = historyService.createHistoricProcessInstanceQuery().finished();
                if (definitionKey != null && !definitionKey.isBlank()) {
                    historyQuery.processDefinitionKey(definitionKey);
                }
                if (businessKey != null && !businessKey.isBlank()) {
                    historyQuery.processInstanceBusinessKey(businessKey);
                }
                List<HistoricProcessInstance> finishedInstances = historyQuery.orderByProcessInstanceEndTime().desc().list();
                for (HistoricProcessInstance instance : finishedInstances) {
                    Map<String, Object> item = new HashMap<>();
                    item.put(SystemConstants.WorkflowResponseKey.ID, instance.getId());
                    item.put(SystemConstants.WorkflowResponseKey.PROCESS_DEFINITION_ID, instance.getProcessDefinitionId());
                    item.put(SystemConstants.WorkflowResponseKey.KEY, instance.getProcessDefinitionKey());
                    item.put(SystemConstants.WorkflowResponseKey.NAME, instance.getName());
                    item.put(SystemConstants.WorkflowResponseKey.BUSINESS_KEY, instance.getBusinessKey());
                    item.put(SystemConstants.WorkflowResponseKey.START_TIME, instance.getStartTime());
                    item.put(SystemConstants.WorkflowResponseKey.END_TIME, instance.getEndTime());
                    item.put(SystemConstants.WorkflowResponseKey.STATUS, SystemConstants.WorkflowNodeStatus.COMPLETED);
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("查询流程实例列表失败", e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.QUERY_PROCESS_INSTANCES_FAILED, e);
        }
    }

    @Override
    public ProcessInstanceVO getInstanceDetail(String id) {
        ProcessInstance running = getProcessInstance(id);
        if (running != null) return ProcessInstanceVO.from(running);
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery().processInstanceId(id).singleResult();
        if (historic == null) throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_INSTANCE_NOT_EXIST);
        return ProcessInstanceVO.from(historic);
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
    public Map<String, Object> getProcessInstanceDiagram(String processInstanceId) {
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
        List<String> highlightedActivities = new ArrayList<>(completedIds);
        highlightedActivities.addAll(activeIds);
        List<String> highlightedFlows = getHighLightedFlows(model, historicActivities);
        ProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        InputStream diagramStream = diagramGenerator.generateDiagram(
                model,
                highlightedActivities,
                highlightedFlows,
                "Arial",
                "Arial",
                "Arial",
                false,
                null
        );
        if (diagramStream == null) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_DIAGRAM_NOT_EXIST);
        }
        try (InputStream inputStream = diagramStream) {
            return buildContentResult(inputStream, SystemConstants.WorkflowValue.CONTENT_TYPE_PNG);
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.WorkflowMessage.GET_DEFINITION_DIAGRAM_FAILED, e);
        }
    }

    @Override
    public List<Map<String, Object>> getProcessInstanceHistory(String processInstanceId) {
        List<Map<String, Object>> items = new ArrayList<>();
        List<org.activiti.engine.history.HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();
        for (org.activiti.engine.history.HistoricTaskInstance task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put(SystemConstants.WorkflowResponseKey.ID, task.getId());
            item.put(SystemConstants.WorkflowResponseKey.NAME, task.getName());
            item.put(SystemConstants.WorkflowResponseKey.ASSIGNEE, task.getAssignee());
            item.put(SystemConstants.WorkflowResponseKey.START_TIME, task.getStartTime());
            item.put(SystemConstants.WorkflowResponseKey.END_TIME, task.getEndTime());
            item.put(SystemConstants.WorkflowResponseKey.STATUS,
                    task.getEndTime() == null ? SystemConstants.WorkflowNodeStatus.ACTIVE : SystemConstants.WorkflowNodeStatus.COMPLETED);
            if (task.getAssignee() != null) {
                Long assigneeId = parseUserId(task.getAssignee());
                var assigneeUser = assigneeId == null ? userService.getUserByUsername(task.getAssignee()) : userService.getById(assigneeId);
                if (assigneeUser != null) {
                    item.put(SystemConstants.WorkflowResponseKey.ASSIGNEE_ID, assigneeUser.getId());
                    String assigneeName = assigneeUser.getNickname();
                    if (assigneeName == null || assigneeName.isBlank()) {
                        assigneeName = assigneeUser.getUsername();
                    }
                    item.put(SystemConstants.WorkflowResponseKey.ASSIGNEE_NAME, assigneeName);
                    item.put(SystemConstants.WorkflowResponseKey.ROLE_CODES, userService.getUserRoles(assigneeUser.getId()));
                } else if (assigneeId != null) {
                    item.put(SystemConstants.WorkflowResponseKey.ASSIGNEE_ID, assigneeId);
                    item.put(SystemConstants.WorkflowResponseKey.ROLE_CODES, userService.getUserRoles(assigneeId));
                }
            }
            List<Comment> comments = taskService.getTaskComments(task.getId());
            if (comments != null && !comments.isEmpty()) {
                List<Map<String, Object>> commentList = new ArrayList<>();
                for (Comment comment : comments) {
                    Map<String, Object> commentItem = new HashMap<>();
                    commentItem.put(SystemConstants.WorkflowResponseKey.USER_ID, comment.getUserId());
                    if (comment.getUserId() != null && !comment.getUserId().isBlank()) {
                        Long commentUserId = parseUserId(comment.getUserId());
                        var commentUser = commentUserId == null ? userService.getUserByUsername(comment.getUserId())
                                : userService.getById(commentUserId);
                        if (commentUser != null) {
                            String commentUserName = commentUser.getNickname();
                            if (commentUserName == null || commentUserName.isBlank()) {
                                commentUserName = commentUser.getUsername();
                            }
                            commentItem.put(SystemConstants.WorkflowResponseKey.USER_NAME, commentUserName);
                            commentItem.put(SystemConstants.WorkflowResponseKey.ROLE_CODES, userService.getUserRoles(commentUser.getId()));
                        } else if (commentUserId != null) {
                            commentItem.put(SystemConstants.WorkflowResponseKey.ROLE_CODES, userService.getUserRoles(commentUserId));
                        }
                    }
                    commentItem.put(SystemConstants.WorkflowResponseKey.TIME, comment.getTime());
                    commentItem.put(SystemConstants.WorkflowResponseKey.MESSAGE, comment.getFullMessage());
                    commentList.add(commentItem);
                }
                item.put(SystemConstants.WorkflowResponseKey.COMMENTS, commentList);
            }
            items.add(item);
        }
        return items;
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
        return getInstanceDetail(processInstanceId).ended();
    }

    @Override
    public void endProcess(String processInstanceId) {
        try {
            ProcessInstance processInstance = getProcessInstance(processInstanceId);
            if (processInstance != null) {
                runtimeService.deleteProcessInstance(processInstanceId, SystemConstants.WorkflowValue.DEFAULT_END_REASON);
                log.info("结束流程成功，流程实例ID: {}", processInstanceId);
            } else {
                getInstanceDetail(processInstanceId);
            }
        } catch (Exception e) {
            log.error("结束流程失败，流程实例ID: {}", processInstanceId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.END_PROCESS_FAILED, e);
        }
    }

    private void requireOwnTaskList(String requested, Long actor, String username) {
        if (!actor.toString().equals(requested) && !username.equals(requested))
            throw new IceeBootException("只能查询自己的待办");
    }

    private Long requireActor() {
        Long actor = TokenUtils.getCurrentUserId();
        if (actor == null) throw new IceeBootException(ResponseMessage.Common.UNAUTHORIZED);
        return actor;
    }

    private String actorName(Long actor) {
        var user = userService.getById(actor);
        if (user == null) throw new IceeBootException(ResponseMessage.Auth.USER_NOT_EXIST);
        return user.getUsername();
    }

    private void requireAssignee(Task task) {
        String assigned = task.getAssignee();
        Long actor = requireActor();
        if (assigned == null || !(assigned.equals(actor.toString()) || assigned.equals(actorName(actor)))) {
            throw new IceeBootException("只有任务办理人可以执行此操作");
        }
    }

    private void completeTask(String taskId, Map<String, Object> variables, String comment) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new IceeBootException(ResponseMessage.WorkflowMessage.TASK_NOT_EXIST_PREFIX + taskId);
            }
            requireAssignee(task);
            if (comment != null && !comment.isBlank()) {
                String previous = Authentication.getAuthenticatedUserId();
                Authentication.setAuthenticatedUserId(requireActor().toString());
                try {
                    taskService.addComment(taskId, task.getProcessInstanceId(), comment);
                } finally {
                    Authentication.setAuthenticatedUserId(previous);
                }
            }
            taskService.complete(taskId, variables);
            log.info("完成任务成功，任务ID: {}, 任务名称: {}", taskId, task.getName());
        } catch (Exception e) {
            log.error("完成任务失败，任务ID: {}", taskId, e);
            throw new IceeBootException(ResponseMessage.WorkflowMessage.COMPLETE_TASK_FAILED, e);
        }
    }

    private void addTasks(Map<String, Task> taskMap, List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (Task task : tasks) {
            taskMap.putIfAbsent(task.getId(), task);
        }
    }

    private Long parseUserId(String value) {
        try {
            return Long.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> getHighLightedFlows(BpmnModel model, List<HistoricActivityInstance> historicActivityInstances) {
        List<String> highFlows = new ArrayList<>();
        if (historicActivityInstances == null || historicActivityInstances.isEmpty()) {
            return highFlows;
        }
        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
            HistoricActivityInstance current = historicActivityInstances.get(i);
            FlowNode currentNode = getFlowNode(model, current.getActivityId());
            if (currentNode == null) {
                continue;
            }
            List<SequenceFlow> outgoingFlows = currentNode.getOutgoingFlows();
            if (outgoingFlows == null || outgoingFlows.isEmpty()) {
                continue;
            }
            List<HistoricActivityInstance> sameStartTimeNodes = new ArrayList<>();
            HistoricActivityInstance nextNode = historicActivityInstances.get(i + 1);
            sameStartTimeNodes.add(nextNode);
            for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
                HistoricActivityInstance node1 = historicActivityInstances.get(j);
                HistoricActivityInstance node2 = historicActivityInstances.get(j + 1);
                if (node1.getStartTime() != null && node2.getStartTime() != null
                        && node1.getStartTime().equals(node2.getStartTime())) {
                    sameStartTimeNodes.add(node2);
                } else {
                    break;
                }
            }
            for (SequenceFlow sequenceFlow : outgoingFlows) {
                String targetRef = sequenceFlow.getTargetRef();
                for (HistoricActivityInstance candidate : sameStartTimeNodes) {
                    if (candidate.getActivityId().equals(targetRef)) {
                        highFlows.add(sequenceFlow.getId());
                        break;
                    }
                }
            }
        }
        return highFlows;
    }

    private FlowNode getFlowNode(BpmnModel model, String activityId) {
        if (model == null || activityId == null) {
            return null;
        }
        for (org.activiti.bpmn.model.Process process : model.getProcesses()) {
            FlowElement element = process.getFlowElement(activityId, true);
            if (element instanceof FlowNode flowNode) {
                return flowNode;
            }
        }
        return null;
    }
}
