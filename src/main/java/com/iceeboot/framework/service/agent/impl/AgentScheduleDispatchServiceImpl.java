package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.service.agent.IAgentScheduleDispatchService;
import com.iceeboot.framework.service.agent.schedule.AgentSchedulePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/** 数据库行锁和唯一计划时间保证多实例不会重复领取；崩溃记录不会自动重试。 */
@Service
@RequiredArgsConstructor
public class AgentScheduleDispatchServiceImpl implements IAgentScheduleDispatchService {
    private final AgentScheduleMapper schedules;
    private final AgentScheduleExecutionMapper executions;
    private final AgentSchedulePolicy policy;
    private final AgentProperties properties;

    @Override
    public List<Long> due() { return schedules.due(System.currentTimeMillis()); }

    @Override
    public List<Long> expired() { return schedules.expired(System.currentTimeMillis()); }

    private void recoverLocked(AgentScheduleDO row, long now) {
        if (row.getRunningId() == null || row.getLeaseUntil() > now) return;
        AgentScheduleExecutionDO stale = new AgentScheduleExecutionDO();
        stale.setId(row.getRunningId());
        stale.setStatus(AgentScheduleExecutionStatus.INTERRUPTED);
        stale.setMessage(ResponseMessage.Agent.RUN_INTERRUPTED);
        stale.setFinishedAt(now);
        executions.finish(stale);
        row.setRunningId(null);
        row.setLeaseUntil(0);
        schedules.update(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recover(long id) {
        AgentScheduleDO row = schedules.lock(id);
        if (row != null) recoverLocked(row, System.currentTimeMillis());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentScheduleExecutionDO claim(long id) {
        AgentScheduleDO row = schedules.lock(id);
        long now = System.currentTimeMillis();
        if (row == null || row.isDeleted() || !row.isEnabled()
                || row.getNextFireAt() == null || row.getNextFireAt() > now) return null;
        recoverLocked(row, now);
        AgentScheduleExecutionDO execution = new AgentScheduleExecutionDO();
        execution.setId(UUID.randomUUID().toString());
        execution.setScheduleId(id);
        execution.setOwnerId(row.getOwnerId());
        execution.setAgentCode(row.getAgentCode());
        execution.setInput(row.getInput());
        execution.setScheduledAt(row.getNextFireAt());
        execution.setStartedAt(now);
        execution.setSessionId("schedule-" + execution.getId());
        execution.setStatus(AgentScheduleExecutionStatus.RUNNING);
        execution.setMessage("");
        if (row.getRunningId() != null) {
            execution.setStatus(AgentScheduleExecutionStatus.SKIPPED_OVERLAP);
            execution.setMessage(ResponseMessage.Agent.SCHEDULE_OVERLAP);
        } else if (row.getScheduleType() == AgentScheduleType.CRON && now - row.getNextFireAt() > 60000) {
            execution.setStatus(AgentScheduleExecutionStatus.SKIPPED_MISFIRE);
            execution.setMessage(ResponseMessage.Agent.SCHEDULE_MISFIRE);
        }
        Long next = policy.afterFire(row, now);
        row.setNextFireAt(next);
        row.setEnabled(next != null);
        if (execution.getStatus() == AgentScheduleExecutionStatus.RUNNING) {
            row.setRunningId(execution.getId());
            row.setLeaseUntil(now + (properties.getExecutionTimeoutSeconds() + 180L) * 1000);
        } else execution.setFinishedAt(now);
        executions.insert(execution);
        schedules.update(row);
        return execution.getStatus() == AgentScheduleExecutionStatus.RUNNING ? execution : null;
    }

    @Override
    public void bindRun(String id, String run) { executions.bindRun(id, run); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(AgentScheduleExecutionDO execution, AgentScheduleExecutionStatus status, String message) {
        AgentScheduleDO row = schedules.lock(execution.getScheduleId());
        execution.setStatus(status);
        execution.setMessage(message == null ? "" : message.substring(0, Math.min(240, message.length())));
        execution.setFinishedAt(System.currentTimeMillis());
        executions.finish(execution);
        if (row != null && execution.getId().equals(row.getRunningId())) {
            row.setRunningId(null);
            row.setLeaseUntil(0);
            schedules.update(row);
        }
    }
}
