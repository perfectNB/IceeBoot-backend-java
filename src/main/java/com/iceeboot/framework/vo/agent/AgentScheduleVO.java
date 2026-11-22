package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.AgentScheduleType;

public record AgentScheduleVO(long id, String agentCode, String name, String input,
        AgentScheduleType scheduleType, String cronExpression, String zoneId, Long executeAt,
        Long nextFireAt, boolean enabled, boolean running) {}
