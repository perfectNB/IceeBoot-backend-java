package com.iceeboot.config.agent;

import com.iceeboot.framework.job.MemoryExtractionJob;

import com.iceeboot.framework.job.AgentScheduleDispatchJob;
import org.quartz.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;

/** 应用初始化和权限迁移完成后启动扫描，不新增第二套调度框架。 */
@Configuration
public class AgentScheduleConfig {
    @Bean("agentScheduleExecutor")
    public ThreadPoolTaskExecutor agentScheduleExecutor(AgentProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getScheduleConcurrency());
        executor.setMaxPoolSize(properties.getScheduleConcurrency());
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("agent-schedule-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean
    public ScheduleStarter agentScheduleStarter(Scheduler scheduler) {
        return new ScheduleStarter(scheduler);
    }

    public record ScheduleStarter(Scheduler scheduler) {
        @EventListener(ApplicationReadyEvent.class)
        public void start() {
            try {
                JobKey key = JobKey.jobKey("agent-schedule-dispatch", "agent-system");
                JobKey memoryKey = JobKey.jobKey("agent-memory-extraction", "agent-system");
                if (!scheduler.checkExists(memoryKey)) {
                    scheduler.scheduleJob(JobBuilder.newJob(MemoryExtractionJob.class)
                                    .withIdentity(memoryKey).build(),
                            TriggerBuilder.newTrigger().withIdentity("agent-memory-scan", "agent-system")
                                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(3)
                                            .repeatForever().withMisfireHandlingInstructionNextWithExistingCount())
                                    .startNow().build());
                }
                if (scheduler.checkExists(key)) return;
                scheduler.scheduleJob(JobBuilder.newJob(AgentScheduleDispatchJob.class).withIdentity(key).build(),
                        TriggerBuilder.newTrigger().withIdentity("agent-schedule-scan", "agent-system")
                                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10)
                                        .repeatForever().withMisfireHandlingInstructionNextWithExistingCount())
                                .startNow().build());
            } catch (SchedulerException error) {
                throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_START_FAILED, error);
            }
        }
    }
}
