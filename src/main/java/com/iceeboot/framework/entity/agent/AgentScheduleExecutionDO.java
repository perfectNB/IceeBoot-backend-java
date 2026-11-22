package com.iceeboot.framework.entity.agent;

import com.iceeboot.framework.enums.agent.AgentScheduleExecutionStatus;
import lombok.Data;

/** 每个计划时间仅认领一次；保留触发时的输入与执行身份快照。 */
@Data
public class AgentScheduleExecutionDO {
    private String id;
    private Long scheduleId;
    private Long ownerId;
    private String agentCode;
    private String input;
    private long scheduledAt;
    private long startedAt;
    private Long finishedAt;
    private AgentScheduleExecutionStatus status;
    private String message;
    private String runId;
    private String sessionId;
}
