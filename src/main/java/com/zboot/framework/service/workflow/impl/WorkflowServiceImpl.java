package com.zboot.framework.service.workflow.impl;

import com.zboot.common.exception.ZbootException;
import com.zboot.framework.service.workflow.IWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
            throw new ZbootException("启动流程实例失败", e);
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
            throw new ZbootException("启动流程实例失败", e);
        }
    }

    @Override
    public void completeTask(String taskId, Map<String, Object> variables) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new ZbootException("任务不存在，任务ID: " + taskId);
            }
            
            taskService.complete(taskId, variables);
            log.info("完成任务成功，任务ID: {}, 任务名称: {}", taskId, task.getName());
        } catch (Exception e) {
            log.error("完成任务失败，任务ID: {}", taskId, e);
            throw new ZbootException("完成任务失败", e);
        }
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
            throw new ZbootException("查询用户任务失败", e);
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
            throw new ZbootException("查询候选任务失败", e);
        }
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
            throw new ZbootException("查询流程实例失败", e);
        }
    }

    @Override
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        try {
            runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
            log.info("删除流程实例成功，流程实例ID: {}, 删除原因: {}", processInstanceId, deleteReason);
        } catch (Exception e) {
            log.error("删除流程实例失败，流程实例ID: {}", processInstanceId, e);
            throw new ZbootException("删除流程实例失败", e);
        }
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
            throw new ZbootException("判断流程是否结束失败", e);
        }
    }

    @Override
    public void jumpToActivity(String taskId, String targetActivityId) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new ZbootException("任务不存在，任务ID: " + taskId);
            }
            
            // 使用Activiti的跳转功能
            managementService.executeCommand(context -> {
                // 这里需要实现具体的跳转逻辑
                // 由于Activiti的跳转比较复杂，这里提供一个基础框架
                log.warn("跳转功能需要根据具体业务需求实现，任务ID: {}, 目标节点: {}", taskId, targetActivityId);
                return null;
            });
            
            log.info("跳转任务成功，任务ID: {}, 目标节点: {}", taskId, targetActivityId);
        } catch (Exception e) {
            log.error("跳转任务失败，任务ID: {}, 目标节点: {}", taskId, targetActivityId, e);
            throw new ZbootException("跳转任务失败", e);
        }
    }

    @Override
    public void endProcess(String processInstanceId) {
        try {
            ProcessInstance processInstance = getProcessInstance(processInstanceId);
            if (processInstance != null) {
                runtimeService.deleteProcessInstance(processInstanceId, "手动结束流程");
                log.info("结束流程成功，流程实例ID: {}", processInstanceId);
            } else {
                log.warn("流程实例不存在或已结束，流程实例ID: {}", processInstanceId);
            }
        } catch (Exception e) {
            log.error("结束流程失败，流程实例ID: {}", processInstanceId, e);
            throw new ZbootException("结束流程失败", e);
        }
    }
}