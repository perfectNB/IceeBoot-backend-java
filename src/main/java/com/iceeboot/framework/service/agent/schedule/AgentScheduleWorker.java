package com.iceeboot.framework.service.agent.schedule;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.entity.agent.AgentScheduleExecutionDO;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.support.AgentDiagnostics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/** 后台执行适配器，复用既有会话、额度、取消和执行过程记录。 */
@Component
@RequiredArgsConstructor
public class AgentScheduleWorker {
    private final IAgentExecutionService execution;
    private final IAgentScheduleDispatchService dispatch;
    private final AgentScheduleAccess access;
    private final AgentProperties properties;

    public void run(AgentScheduleExecutionDO task) {
        try {
            access.requireExecution(task.getOwnerId());
            access.requireAgent(task.getAgentCode());
            AtomicBoolean bound = new AtomicBoolean();
            AgentProgress terminal = execution.stream(task.getAgentCode(),
                    new InvokeRequest(task.getInput(), task.getSessionId()),
                    new Caller(task.getOwnerId().toString(), null))
                    .doOnNext(event -> {
                        if (event.rootRunId() != null && bound.compareAndSet(false, true))
                            dispatch.bindRun(task.getId(), event.rootRunId());
                    }).blockLast(Duration.ofSeconds(properties.getExecutionTimeoutSeconds() + 60L));
            if (terminal != null && terminal.type() == AgentProgressType.RUN_FINISHED) {
                dispatch.finish(task, AgentScheduleExecutionStatus.SUCCEEDED, "");
            } else {
                AgentScheduleExecutionStatus status = terminal != null && "CANCELLED".equals(terminal.status())
                        ? AgentScheduleExecutionStatus.CANCELLED
                        : terminal != null && "TIMED_OUT".equals(terminal.status())
                        ? AgentScheduleExecutionStatus.TIMED_OUT : AgentScheduleExecutionStatus.FAILED;
                // 执行层已将第三方错误转换为框架消息；不把未知异常原文存入任务记录。
                dispatch.finish(task, status, terminal != null && terminal.type() == AgentProgressType.ERROR
                        ? terminal.text() : ResponseMessage.Agent.EXECUTION_FAILED);
            }
        } catch (RuntimeException error) {
            AgentDiagnostics.failure(task.getId(), "SCHEDULE", AgentDiagnostics.traceId(), error);
            String message = error instanceof com.iceeboot.common.exception.IceeBootException
                    ? error.getMessage() : ResponseMessage.Agent.EXECUTION_FAILED;
            dispatch.finish(task, AgentScheduleExecutionStatus.FAILED, message);
        }
    }
}
