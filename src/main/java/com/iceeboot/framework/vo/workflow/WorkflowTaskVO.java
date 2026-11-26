package com.iceeboot.framework.vo.workflow;

import java.util.Date;
import org.activiti.engine.task.Task;

public record WorkflowTaskVO(String id, String name, String description, String assignee,
        String owner, String processInstanceId, String processDefinitionId, String taskDefinitionKey,
        String executionId, Date createTime, Date dueDate, int priority) {
    public static WorkflowTaskVO from(Task task) {
        return new WorkflowTaskVO(task.getId(), task.getName(), task.getDescription(), task.getAssignee(),
                task.getOwner(), task.getProcessInstanceId(), task.getProcessDefinitionId(),
                task.getTaskDefinitionKey(), task.getExecutionId(), task.getCreateTime(), task.getDueDate(),
                task.getPriority());
    }
}
