package com.iceeboot.framework.service.workflow.impl;

import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.workflow.ProcessStartDTO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.service.system.ISysUserService;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowInstanceTest {
    @Test
    void archivedInstanceHasDetailsAndAnUnknownIdIsRejected() {
        RuntimeService runtime = mock(RuntimeService.class);
        HistoryService history = mock(HistoryService.class);
        ProcessInstanceQuery live = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        HistoricProcessInstanceQuery archived = mock(HistoricProcessInstanceQuery.class, RETURNS_SELF);
        when(runtime.createProcessInstanceQuery()).thenReturn(live);
        when(history.createHistoricProcessInstanceQuery()).thenReturn(archived);
        var service = new WorkflowServiceImpl(runtime, null, history, null, null);
        assertThatThrownBy(() -> service.isProcessEnded("unknown")).hasMessageContaining("流程实例");
        HistoricProcessInstance finished = mock(HistoricProcessInstance.class);
        when(finished.getId()).thenReturn("done");
        when(finished.getEndTime()).thenReturn(new java.util.Date());
        when(archived.singleResult()).thenReturn(finished);
        assertThat(service.getInstanceDetail("done").ended()).isTrue();
        assertThat(service.getInstanceDetail("done").id()).isEqualTo("done");
    }

    @Test
    void candidateClaimsReleasesAndCompletesAnActualProcess() {
        var engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:workflow_lifecycle;MODE=LEGACY;NON_KEYWORDS=VALUE")
                .setDatabaseSchemaUpdate("true")
                .setAsyncExecutorActivate(false)
                .buildProcessEngine();
        try (var identity = mockStatic(TokenUtils.class)) {
            String xml = """
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                      xmlns:activiti="http://activiti.org/bpmn" targetNamespace="test">
                      <process id="approval" isExecutable="true">
                        <startEvent id="start"/><sequenceFlow id="s1" sourceRef="start" targetRef="review"/>
                        <userTask id="review" name="Review" activiti:candidateUsers="alice"/>
                        <sequenceFlow id="s2" sourceRef="review" targetRef="end"/><endEvent id="end"/>
                      </process>
                    </definitions>
                    """;
            engine.getRepositoryService().createDeployment().addString("approval.bpmn20.xml", xml).deploy();
            var users = mock(ISysUserService.class);
            var alice = new UserDO();
            alice.setUsername("alice");
            when(users.getById(7L)).thenReturn(alice);
            identity.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            identity.when(TokenUtils::getCurrentUserIdAsString).thenReturn("7");
            var service = new WorkflowServiceImpl(engine.getRuntimeService(), engine.getTaskService(),
                    engine.getHistoryService(), engine.getRepositoryService(), users);
            var start = new ProcessStartDTO();
            start.setProcessDefinitionKey("approval");
            String id = service.startProcess(start).getId();
            String task = service.getMyTasks().get(0).getId();
            assertThatThrownBy(() -> service.completeTask(task, null)).isInstanceOf(RuntimeException.class);
            service.claimTask(task, null);
            service.unclaimTask(task);
            service.claimTask(task, null);
            service.completeTask(task, null);
            assertThat(service.getMyTasks()).isEmpty();
            assertThat(service.getInstanceDetail(id).startUserId()).isEqualTo("7");
            assertThat(service.isProcessEnded(id)).isTrue();
        } finally {
            engine.close();
        }
    }
}
