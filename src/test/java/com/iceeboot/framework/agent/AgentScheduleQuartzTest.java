package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.config.agent.*;
import com.iceeboot.config.task.QuartzConfig;
import com.iceeboot.framework.job.AgentScheduleDispatchJob;
import com.iceeboot.framework.service.agent.schedule.AgentScheduleCoordinator;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AgentScheduleQuartzTest {
    @Test
    void quartzCreatesConstructorInjectedJobAndRegistersOnlyOneScanner() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            var coordinator = mock(AgentScheduleCoordinator.class);
            context.registerBean(AgentScheduleCoordinator.class, () -> coordinator);
            context.refresh();
            var source = new DriverManagerDataSource("jdbc:h2:mem:schedule_quartz;MODE=MySQL", "sa", "");
            var factory = new QuartzConfig().schedulerFactoryBean(source, context.getAutowireCapableBeanFactory());
            factory.setApplicationContext(context);
            factory.afterPropertiesSet();
            try {
                var scheduler = factory.getScheduler();
                var starter = new AgentScheduleConfig.ScheduleStarter(scheduler);
                starter.start(); starter.start();
                JobKey key = JobKey.jobKey("agent-schedule-dispatch", "agent-system");
                assertThat(scheduler.checkExists(key)).isTrue();
                assertThat(scheduler.getTriggersOfJob(key)).hasSize(1);
                var job = context.getAutowireCapableBeanFactory().createBean(AgentScheduleDispatchJob.class);
                job.execute(null);
                verify(coordinator).tick();
                assertThat(AgentScheduleDispatchJob.class.isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
            } finally { factory.destroy(); }
        }
    }

    @Test
    void dedicatedExecutorHasBoundedConcurrencyAndNoWaitingQueue() {
        var properties = new AgentProperties(); properties.setScheduleConcurrency(3);
        var executor = new AgentScheduleConfig().agentScheduleExecutor(properties);
        executor.initialize();
        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(3);
            assertThat(executor.getMaxPoolSize()).isEqualTo(3);
            assertThat(executor.getThreadPoolExecutor().getQueue()).isInstanceOf(java.util.concurrent.SynchronousQueue.class);
        } finally { executor.shutdown(); }
    }
}
