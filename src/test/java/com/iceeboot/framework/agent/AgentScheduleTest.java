package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.dto.create.agent.AgentScheduleDTO;
import com.iceeboot.framework.dto.update.agent.AgentScheduleUpdateDTO;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.impl.*;
import com.iceeboot.framework.service.agent.schedule.*;
import com.iceeboot.framework.service.database.AgentSchemaMigrationService;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** 使用真实 Mapper、行锁、唯一索引与事务代理验证认领和恢复边界。 */
class AgentScheduleTest {
    private JdbcTemplate jdbc;
    private AgentScheduleMapper schedules;
    private AgentScheduleExecutionMapper executions;
    private AgentScheduleAccess access;
    private IAgentScheduleService service;
    private IAgentScheduleDispatchService dispatch;
    private DataSourceTransactionManager transactions;
    private final AgentSchedulePolicy policy = new AgentSchedulePolicy();
    private final AgentProperties properties = new AgentProperties();

    @BeforeEach
    void setup() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        jdbc = new JdbcTemplate(source);
        new AgentSchemaMigrationService(source).migrateSchema();
        var factory = new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(AgentScheduleMapper.class);
        configuration.addMapper(AgentScheduleExecutionMapper.class);
        factory.setConfiguration(configuration);
        var session = new SqlSessionTemplate(factory.getObject());
        schedules = session.getMapper(AgentScheduleMapper.class);
        executions = session.getMapper(AgentScheduleExecutionMapper.class);
        transactions = new DataSourceTransactionManager(source);
        access = mock(AgentScheduleAccess.class);
        service = tx(new AgentScheduleServiceImpl(schedules, executions, policy, access));
        dispatch = tx(new AgentScheduleDispatchServiceImpl(schedules, executions, policy, properties));
    }

    @SuppressWarnings("unchecked")
    private <T> T tx(T target) {
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (T) proxy.getProxy();
    }

    private AgentScheduleDTO request() {
        var dto = new AgentScheduleDTO();
        dto.setName("每日摘要"); dto.setAgentCode("daily_agent"); dto.setInput("总结今日进展");
        dto.setScheduleType(AgentScheduleType.ONCE); dto.setZoneId("Asia/Shanghai");
        dto.setExecuteAt(System.currentTimeMillis() + 600000); dto.setEnabled(true);
        return dto;
    }

    private long due(AgentScheduleType type, long offset) {
        var dto = request();
        if (type == AgentScheduleType.CRON) {
            dto.setScheduleType(type); dto.setCronExpression("0 * * * * ?");
        }
        long id = service.create(1, dto).id();
        jdbc.update("UPDATE t_agent_schedule SET next_fire_at=? WHERE id=?", System.currentTimeMillis() - offset, id);
        return id;
    }

    @Test
    void timePolicyHonorsZoneAndRejectsInvalidOrPastSchedules() {
        var row = new AgentScheduleDO(); row.setScheduleType(AgentScheduleType.CRON);
        row.setZoneId("Asia/Shanghai"); row.setCronExpression("0 0 9 ? * MON-FRI");
        long now = Instant.parse("2026-11-23T00:00:00Z").toEpochMilli();
        assertThat(policy.next(row, now, true)).isEqualTo(Instant.parse("2026-11-23T01:00:00Z").toEpochMilli());
        row.setZoneId("UTC");
        assertThat(policy.next(row, now, true)).isEqualTo(Instant.parse("2026-11-23T09:00:00Z").toEpochMilli());
        for (String cron : List.of("*/5 * * * * ?", "0 invalid", "0 0 0 32 * ?")) {
            row.setCronExpression(cron);
            assertThatThrownBy(() -> policy.next(row, now, true)).isInstanceOf(IceeBootException.class);
        }
        row.setCronExpression("0 0 9 ? * *"); row.setZoneId("bad/zone");
        assertThatThrownBy(() -> policy.next(row, now, true)).hasMessage(ResponseMessage.Agent.SCHEDULE_CRON_INVALID);
        row.setZoneId("UTC"); row.setScheduleType(AgentScheduleType.ONCE); row.setExecuteAt(now - 1);
        assertThatThrownBy(() -> policy.next(row, now, true)).hasMessage(ResponseMessage.Agent.SCHEDULE_TIME_INVALID);
        assertThat(policy.afterFire(row, now)).isNull();
    }

    @Test
    void concurrentNodesClaimOnceAndPersistSnapshotWithoutAnAccessToken() throws Exception {
        long id = due(AgentScheduleType.ONCE, 1000);
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<AgentScheduleExecutionDO> claim = () -> { gate.await(); return dispatch.claim(id); };
            var first = pool.submit(claim); var second = pool.submit(claim); gate.countDown();
            var results = Arrays.asList(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(Objects::nonNull).toList()).singleElement().satisfies(run -> {
                assertThat(run.getOwnerId()).isEqualTo(1);
                assertThat(run.getAgentCode()).isEqualTo("daily_agent");
                assertThat(run.getInput()).isEqualTo("总结今日进展");
                assertThat(run.getSessionId()).startsWith("schedule-");
            });
        }
        assertThat(executions.list(id, 1, 0)).hasSize(1);
        assertThat(dispatch.claim(id)).isNull();
        assertThat(service.list(1)).singleElement().satisfies(task -> {
            assertThat(task.enabled()).isFalse(); assertThat(task.nextFireAt()).isNull(); assertThat(task.running()).isTrue();
        });
    }

    @Test
    void overlappingAndMissedCyclesAreRecordedAndAdvancedWithoutModelCalls() {
        long overlap = due(AgentScheduleType.CRON, 1000);
        jdbc.update("UPDATE t_agent_schedule SET running_id='previous',lease_until=? WHERE id=?", System.currentTimeMillis() + 600000, overlap);
        assertThat(dispatch.claim(overlap)).isNull();
        assertThat(executions.list(overlap, 1, 0)).singleElement().satisfies(run ->
                assertThat(run.getStatus()).isEqualTo(AgentScheduleExecutionStatus.SKIPPED_OVERLAP));
        long missed = due(AgentScheduleType.CRON, 300000);
        assertThat(dispatch.claim(missed)).isNull();
        assertThat(executions.list(missed, 1, 0)).singleElement().satisfies(run ->
                assertThat(run.getStatus()).isEqualTo(AgentScheduleExecutionStatus.SKIPPED_MISFIRE));
        assertThat(dispatch.due()).doesNotContain(overlap, missed);
        assertThat(dispatch.claim(missed)).isNull();
    }

    @Test
    void restartRecoversStaleRunButLateCompletionCannotOverwriteNewRun() {
        long id = due(AgentScheduleType.CRON, 1000);
        var stale = dispatch.claim(id);
        jdbc.update("UPDATE t_agent_schedule SET next_fire_at=?,lease_until=0 WHERE id=?", stale.getScheduledAt() + 1, id);
        assertThat(dispatch.expired()).contains(id);
        dispatch.recover(id);
        assertThat(executions.list(id, 1, 0)).singleElement().satisfies(run ->
                assertThat(run.getStatus()).isEqualTo(AgentScheduleExecutionStatus.INTERRUPTED));
        var next = dispatch.claim(id);
        assertThat(next).isNotNull();
        dispatch.finish(stale, AgentScheduleExecutionStatus.SUCCEEDED, "late result");
        assertThat(schedules.owned(id, 1).getRunningId()).isEqualTo(next.getId());
        assertThat(executions.list(id, 1, 0).stream().filter(run -> run.getId().equals(stale.getId())).findFirst().orElseThrow().getStatus())
                .isEqualTo(AgentScheduleExecutionStatus.INTERRUPTED);
        dispatch.finish(next, AgentScheduleExecutionStatus.SUCCEEDED, "");
        assertThat(schedules.owned(id, 1).getRunningId()).isNull();
    }

    @Test
    void claimFailureRollsBackBothRecoveryAndScheduleChanges() {
        long id = due(AgentScheduleType.CRON, 1000);
        var first = dispatch.claim(id);
        long dueAt = System.currentTimeMillis() - 1000;
        jdbc.update("UPDATE t_agent_schedule SET next_fire_at=?,lease_until=0 WHERE id=?", dueAt, id);
        var failing = spy(executions);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("simulated")).when(failing).insert(any());
        IAgentScheduleDispatchService broken = tx(new AgentScheduleDispatchServiceImpl(schedules, failing, policy, properties));
        assertThatThrownBy(() -> broken.claim(id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        var row = schedules.owned(id, 1);
        assertThat(row.getRunningId()).isEqualTo(first.getId());
        assertThat(row.getNextFireAt()).isEqualTo(dueAt);
        assertThat(executions.list(id, 1, 0)).singleElement().satisfies(run ->
                assertThat(run.getStatus()).isEqualTo(AgentScheduleExecutionStatus.RUNNING));
    }

    @Test
    void ownerIsolationAndPausePreserveAnActiveRunWithoutReschedulingOnFinish() {
        long id = due(AgentScheduleType.CRON, 1000);
        var run = dispatch.claim(id);
        assertThatThrownBy(() -> service.enabled(2, id, false)).hasMessage(ResponseMessage.Agent.SCHEDULE_NOT_FOUND);
        assertThatThrownBy(() -> service.executions(2, id, 0)).hasMessage(ResponseMessage.Agent.SCHEDULE_NOT_FOUND);
        assertThatThrownBy(() -> service.delete(1, id)).hasMessage(ResponseMessage.Agent.SCHEDULE_BUSY);
        var update = new AgentScheduleUpdateDTO(); update.setId(id);
        assertThatThrownBy(() -> service.update(1, update)).hasMessage(ResponseMessage.Agent.SCHEDULE_BUSY);
        service.enabled(1, id, false);
        assertThat(schedules.owned(id, 1).getRunningId()).isEqualTo(run.getId());
        dispatch.finish(run, AgentScheduleExecutionStatus.SUCCEEDED, "");
        assertThat(service.list(1)).singleElement().satisfies(task -> {
            assertThat(task.enabled()).isFalse(); assertThat(task.running()).isFalse(); assertThat(task.nextFireAt()).isNull();
        });
        assertThatThrownBy(() -> service.executions(1, id, -1)).hasMessage(ResponseMessage.Agent.REQUEST_INVALID);
        service.delete(1, id);
        assertThat(service.list(1)).isEmpty();
        assertThat(executions.list(id, 1, 0)).hasSize(1);
    }

    @Test
    void pausedSchedulesDoNotFireAndOneTimeSchedulesCannotBeReenabledAfterTheirTime() {
        var dto = request(); dto.setEnabled(false);
        long id = service.create(1, dto).id();
        assertThat(dispatch.claim(id)).isNull();
        assertThat(schedules.owned(id, 1).getNextFireAt()).isNull();
        jdbc.update("UPDATE t_agent_schedule SET execute_at=1 WHERE id=?", id);
        assertThatThrownBy(() -> service.enabled(1, id, true)).hasMessage(ResponseMessage.Agent.SCHEDULE_TIME_INVALID);
        assertThat(schedules.owned(id, 1).isEnabled()).isFalse();
    }

    @Test
    void revokedExecutionPermissionPreventsCreatingOrEnablingSchedules() {
        doThrow(new IceeBootException(403, ResponseMessage.Agent.SCHEDULE_PERMISSION_DENIED))
                .when(access).requireExecution(1);
        assertThatThrownBy(() -> service.create(1, request())).hasMessage(ResponseMessage.Agent.SCHEDULE_PERMISSION_DENIED);
        assertThat(service.list(1)).isEmpty();
    }
}
