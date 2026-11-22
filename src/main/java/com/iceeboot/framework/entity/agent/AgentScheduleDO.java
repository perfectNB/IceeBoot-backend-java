package com.iceeboot.framework.entity.agent;

import com.iceeboot.framework.enums.agent.AgentScheduleType;
import lombok.Data;

/** 调度配置持久化记录，所有时间点使用 UTC 毫秒，不保存登录令牌。 */
@Data
public class AgentScheduleDO {
    private Long id;
    private Long ownerId;
    private String agentCode;
    private String name;
    private String input;
    private AgentScheduleType scheduleType;
    private String cronExpression;
    private String zoneId;
    private Long executeAt;
    private Long nextFireAt;
    private boolean enabled;
    private boolean deleted;
    private String runningId;
    private long leaseUntil;
}
