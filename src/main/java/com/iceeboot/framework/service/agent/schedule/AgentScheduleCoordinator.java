package com.iceeboot.framework.service.agent.schedule;

import com.iceeboot.framework.service.agent.support.AgentDiagnostics;
import com.iceeboot.framework.service.agent.IAgentScheduleDispatchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/** 短时扫描与长时模型调用隔离，线程池满时保留未认领计划，避免丢失单次任务和无界排队。 */
@Component
public class AgentScheduleCoordinator {
    private final IAgentScheduleDispatchService dispatch;
    private final AgentScheduleWorker worker;
    private final TaskExecutor executor;

    public AgentScheduleCoordinator(IAgentScheduleDispatchService dispatch, AgentScheduleWorker worker,
            @Qualifier("agentScheduleExecutor") TaskExecutor executor) {
        this.dispatch = dispatch;
        this.worker = worker;
        this.executor = executor;
    }

    public void tick() {
        for (long id : dispatch.expired()) {
            try { dispatch.recover(id); }
            catch (RuntimeException error) { diagnose(id, error); }
        }
        for (long id : dispatch.due()) {
            try { executor.execute(() -> run(id)); }
            catch (org.springframework.core.task.TaskRejectedException full) { break; }
        }
    }

    private void run(long id) {
        try {
            // 在线程真正开始执行后才认领，等待容量期间不占运行租约。
            var task = dispatch.claim(id);
            if (task != null) worker.run(task);
        } catch (RuntimeException error) { diagnose(id, error); }
    }

    private void diagnose(long id, RuntimeException error) {
        AgentDiagnostics.failure(Long.toString(id), "SCHEDULE_DISPATCH", AgentDiagnostics.traceId(), error);
    }
}
