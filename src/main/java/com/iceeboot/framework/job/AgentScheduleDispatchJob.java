package com.iceeboot.framework.job;

import com.iceeboot.framework.service.agent.schedule.AgentScheduleCoordinator;
import lombok.RequiredArgsConstructor;
import org.quartz.*;

/** Quartz 仅唤醒数据库扫描，业务计划与认领记录均在数据库。 */
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class AgentScheduleDispatchJob implements Job {
    private final AgentScheduleCoordinator coordinator;

    @Override
    public void execute(JobExecutionContext context) { coordinator.tick(); }
}
