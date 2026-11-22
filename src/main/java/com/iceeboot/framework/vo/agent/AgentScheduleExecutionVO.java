package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.AgentScheduleExecutionStatus;

public record AgentScheduleExecutionVO(String id, long scheduledAt, long startedAt, Long finishedAt,
        AgentScheduleExecutionStatus status, String message, String runId, String sessionId) {}
