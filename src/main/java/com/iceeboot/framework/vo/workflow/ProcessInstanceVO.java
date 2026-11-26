package com.iceeboot.framework.vo.workflow;

import java.util.Date;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.history.HistoricProcessInstance;

public record ProcessInstanceVO(String id, String processInstanceId, String processDefinitionId,
        String processDefinitionKey, String processDefinitionName, Integer processDefinitionVersion,
        String businessKey, String name, String activityId, String startUserId, Date startTime,
        Date endTime, boolean ended) {
    public static ProcessInstanceVO from(ProcessInstance p) {
        return new ProcessInstanceVO(p.getId(), p.getId(), p.getProcessDefinitionId(), p.getProcessDefinitionKey(),
                p.getProcessDefinitionName(), p.getProcessDefinitionVersion(), p.getBusinessKey(), p.getName(),
                p.getActivityId(), p.getStartUserId(), p.getStartTime(), null, p.isEnded());
    }

    public static ProcessInstanceVO from(HistoricProcessInstance p) {
        return new ProcessInstanceVO(p.getId(), p.getId(), p.getProcessDefinitionId(), p.getProcessDefinitionKey(),
                p.getProcessDefinitionName(), p.getProcessDefinitionVersion(), p.getBusinessKey(), p.getName(),
                p.getEndActivityId(), p.getStartUserId(), p.getStartTime(), p.getEndTime(), p.getEndTime() != null);
    }
}
