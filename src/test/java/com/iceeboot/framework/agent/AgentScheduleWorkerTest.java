package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.*;
import com.iceeboot.framework.entity.agent.AgentScheduleExecutionDO;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.schedule.*;
import com.iceeboot.framework.service.system.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import reactor.core.publisher.Flux;
import java.util.List;

class AgentScheduleWorkerTest {
    private final IAgentExecutionService execution = mock(IAgentExecutionService.class);
    private final IAgentScheduleDispatchService dispatch = mock(IAgentScheduleDispatchService.class);
    private final AgentScheduleAccess access = mock(AgentScheduleAccess.class);
    private final AgentScheduleWorker worker = new AgentScheduleWorker(execution, dispatch, access, new AgentProperties());

    private AgentScheduleExecutionDO task() {
        var task = new AgentScheduleExecutionDO(); task.setId("occurrence"); task.setScheduleId(1L);
        task.setOwnerId(7L); task.setAgentCode("daily"); task.setInput("report"); task.setSessionId("schedule-occurrence");
        return task;
    }

    private AgentProgress event(AgentProgressType type, String status) {
        return new AgentProgress(1, type, "run1", "run1", "", "daily", "", "safe message", "", "", status, 0);
    }

    @Test
    void usesTrustedOwnerWithoutPersistingOrReusingALoginToken() {
        var task = task();
        when(execution.stream(eq("daily"), any(), any())).thenReturn(Flux.just(
                event(AgentProgressType.RUN_STARTED, "RUNNING"), event(AgentProgressType.RUN_FINISHED, "SUCCEEDED")));
        worker.run(task);
        verify(execution).stream(eq("daily"), eq(new InvokeRequest("report", "schedule-occurrence")), eq(new Caller("7", null)));
        verify(dispatch).bindRun("occurrence", "run1");
        verify(dispatch).finish(task, AgentScheduleExecutionStatus.SUCCEEDED, "");
    }

    @Test
    void revokedPermissionsPreventAnyModelInvocation() {
        var task = task();
        doThrow(new IceeBootException(403, ResponseMessage.Agent.SCHEDULE_PERMISSION_DENIED)).when(access).requireExecution(7);
        worker.run(task);
        verifyNoInteractions(execution);
        verify(dispatch).finish(task, AgentScheduleExecutionStatus.FAILED, ResponseMessage.Agent.SCHEDULE_PERMISSION_DENIED);
    }

    @Test
    void unknownFailureIsSanitizedAndNeverRetried() {
        var task = task();
        when(execution.stream(anyString(), any(), any())).thenReturn(Flux.error(new RuntimeException("sensitive provider response")));
        worker.run(task);
        verify(execution, times(1)).stream(anyString(), any(), any());
        verify(dispatch).finish(task, AgentScheduleExecutionStatus.FAILED, ResponseMessage.Agent.EXECUTION_FAILED);
    }

    @Test
    void cancellationAndTimeoutRemainDistinctTerminalStates() {
        for (var status : List.of(AgentScheduleExecutionStatus.CANCELLED, AgentScheduleExecutionStatus.TIMED_OUT)) {
            var task = task();
            when(execution.stream(anyString(), any(), any())).thenReturn(Flux.just(event(AgentProgressType.ERROR, status.name())));
            worker.run(task);
            verify(dispatch).finish(task, status, "safe message");
        }
    }

    @Test
    void saturatedExecutorLeavesScheduleUnclaimedForALaterScan() {
        var task = task();
        when(dispatch.expired()).thenReturn(List.of()); when(dispatch.due()).thenReturn(List.of(1L));
        when(dispatch.claim(1)).thenReturn(task);
        TaskExecutor executor = command -> { throw new org.springframework.core.task.TaskRejectedException("full"); };
        new AgentScheduleCoordinator(dispatch, worker, executor).tick();
        verifyNoInteractions(execution);
        verify(dispatch, never()).claim(anyLong());
        verify(dispatch, never()).finish(any(), any(), anyString());
    }

    @Test
    void oneBrokenScheduleDoesNotBlockOtherSchedules() {
        var task = task();
        when(dispatch.expired()).thenReturn(List.of()); when(dispatch.due()).thenReturn(List.of(2L, 1L));
        when(dispatch.claim(2)).thenThrow(new RuntimeException("db error")); when(dispatch.claim(1)).thenReturn(task);
        when(execution.stream(anyString(), any(), any())).thenReturn(Flux.just(event(AgentProgressType.RUN_FINISHED, "SUCCEEDED")));
        new AgentScheduleCoordinator(dispatch, worker, Runnable::run).tick();
        verify(dispatch).finish(task, AgentScheduleExecutionStatus.SUCCEEDED, "");
    }

    @Test
    void liveAuthorizationChecksRejectDisabledAccountsAndMissingInvokeMapping() {
        var auth = mock(IAuthCacheService.class); var users = mock(ISysUserService.class);
        var store = mock(IAgentPersistenceService.class);
        var guard = new AgentScheduleAccess(auth, users, store);
        when(users.getUserRoles(7L)).thenReturn(List.of("superAdmin"));
        assertThatThrownBy(() -> guard.requireExecution(7)).isInstanceOf(IceeBootException.class);
        when(auth.hasUserPermissions(eq(7L), anyList(), eq(true))).thenReturn(true);
        when(auth.getUrlPermissions(anyString(), eq("POST"))).thenReturn(List.of());
        assertThatThrownBy(() -> guard.requireExecution(7)).hasMessage(ResponseMessage.Agent.SCHEDULE_PERMISSION_DENIED);
        when(auth.getUrlPermissions(anyString(), eq("POST"))).thenReturn(List.of("ai:agent:invoke:api"));
        guard.requireExecution(7);
        verify(auth).hasUserPermissions(7L, List.of("ai:agent:invoke:api", "ai:schedule:execute"), true);
        when(users.getUserRoles(7L)).thenReturn(List.of("admin"));
        assertThatThrownBy(() -> guard.requireExecution(7)).hasMessage(ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
    }

    @Test
    void projectToolsInChildAgentsAreRejectedBeforeScheduling() {
        var store = mock(IAgentPersistenceService.class);
        var view = mock(AgentView.class); var parent = mock(PublishedAgent.class); var child = mock(PublishedAgent.class);
        var spec = mock(AgentSpec.class); var capability = mock(Capability.class); var tool = mock(ToolConfig.class);
        when(store.agentByCode("daily")).thenReturn(view); when(view.activeReleaseId()).thenReturn("release");
        when(store.release("release")).thenReturn(parent); when(parent.agent()).thenReturn(spec); when(spec.enabled()).thenReturn(true);
        when(parent.capabilities()).thenReturn(List.of()); when(parent.children()).thenReturn(List.of(child));
        when(child.capabilities()).thenReturn(List.of(capability)); when(capability.tool()).thenReturn(tool); when(tool.projectApi()).thenReturn(true);
        var guard = new AgentScheduleAccess(mock(IAuthCacheService.class), mock(ISysUserService.class), store);
        assertThatThrownBy(() -> guard.requireAgent("daily")).hasMessage(ResponseMessage.Agent.SCHEDULE_PROJECT_TOOL_UNSUPPORTED);
    }
}
