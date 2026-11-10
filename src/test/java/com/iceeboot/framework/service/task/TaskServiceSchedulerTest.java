package com.iceeboot.framework.service.task;

import com.iceeboot.framework.entity.task.TaskDO;
import com.iceeboot.framework.service.task.impl.TaskServiceImpl;
import org.junit.jupiter.api.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TaskServiceSchedulerTest {
    Scheduler scheduler;
    TaskServiceImpl service;
    public static class CountingJob implements Job {
        static volatile CountDownLatch executed;
        public void execute(JobExecutionContext context) { if(executed!=null) executed.countDown(); }
    }
    @BeforeEach void setup() throws Exception {
        Properties properties=new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName","task-regression-"+UUID.randomUUID());
        properties.setProperty("org.quartz.threadPool.threadCount","1");
        properties.setProperty("org.quartz.jobStore.class","org.quartz.simpl.RAMJobStore");
        scheduler=new StdSchedulerFactory(properties).getScheduler();
        service=spy(new TaskServiceImpl(scheduler));
        doReturn(0L).when(service).count(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        doReturn(true).when(service).save(any(TaskDO.class));
        doReturn(true).when(service).updateById(any(TaskDO.class));
    }
    @AfterEach void close() throws Exception { scheduler.shutdown(true); }
    TaskDO task(String name,String status) {
        TaskDO task=new TaskDO(); task.setId(1L);task.setName(name);task.setStatus(status);
        task.setClassName(CountingJob.class.getName());task.setCronExpression("0 0 2 * * ?");
        task.setDescription("scheduler regression");return task;
    }
    void existing(TaskDO task) throws Exception {
        TaskDO saved=new TaskDO();org.springframework.beans.BeanUtils.copyProperties(task,saved);
        doReturn(saved).when(service).getById(task.getId());
        scheduler.scheduleJob(JobBuilder.newJob(CountingJob.class).withIdentity(task.getName()).build(),
                TriggerBuilder.newTrigger().withIdentity(task.getName()+"_trigger")
                    .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression())).build());
    }
    @Test void editingAnEnabledTaskRetainsARealExecutableQuartzJob() throws Exception {
        TaskDO task=task("sampleTask","active"); existing(task);
        service.updateTask(1L,task);
        assertThat(scheduler.checkExists(JobKey.jobKey(task.getName()))).isTrue();
        CountingJob.executed=new CountDownLatch(1);scheduler.start();
        scheduler.triggerJob(JobKey.jobKey(task.getName()));
        assertThat(CountingJob.executed.await(3,TimeUnit.SECONDS)).isTrue();
    }
    @Test void startingADatabaseTaskWhoseQuartzJobIsMissingActuallyCreatesAndExecutesIt() throws Exception {
        TaskDO task=task("sampleTask","inactive");
        doReturn(task).when(service).getById("1");
        service.startTask("1");
        assertThat(scheduler.checkExists(JobKey.jobKey(task.getName()))).isTrue();
        assertThat(task.getStatus()).isEqualTo("active");
        CountingJob.executed=new CountDownLatch(1);scheduler.start();
        scheduler.triggerJob(JobKey.jobKey(task.getName()));
        assertThat(CountingJob.executed.await(3,TimeUnit.SECONDS)).isTrue();
    }
    @Test void entityCreationSchedulesOnlyEnabledTasks() throws Exception {
        service.createTask(task("enabled-archive","active"));
        service.createTask(task("disabled-archive","inactive"));
        assertThat(scheduler.checkExists(JobKey.jobKey("enabled-archive"))).isTrue();
        assertThat(scheduler.checkExists(JobKey.jobKey("disabled-archive"))).isFalse();
    }
    @Test void renamingATaskRemovesTheOldSchedulerKey() throws Exception {
        TaskDO original=task("old-archive","active");existing(original);
        TaskDO renamed=task("new-archive","active");service.updateTask(1L,renamed);
        assertThat(scheduler.checkExists(JobKey.jobKey("old-archive"))).isFalse();
        assertThat(scheduler.checkExists(JobKey.jobKey("new-archive"))).isTrue();
    }
    @Test void invalidCronEditCannotEraseTheWorkingJobOrUpdateTheDatabase() throws Exception {
        TaskDO original=task("sampleTask","active");existing(original);
        TaskDO invalid=task(original.getName(),"active");invalid.setCronExpression("not-a-cron");
        assertThatThrownBy(()->service.updateTask(1L,invalid)).isInstanceOf(RuntimeException.class);
        assertThat(scheduler.checkExists(JobKey.jobKey(original.getName()))).isTrue();
        verify(service,never()).updateById(any(TaskDO.class));
    }
    @Test void failedDatabaseUpdateCannotReplaceTheCurrentTrigger() throws Exception {
        TaskDO original=task("sampleTask","active");existing(original);
        doReturn(false).when(service).updateById(any(TaskDO.class));
        TaskDO changed=task(original.getName(),"active");changed.setCronExpression("0 0 4 * * ?");
        assertThatThrownBy(()->service.updateTask(1L,changed)).isInstanceOf(RuntimeException.class);
        CronTrigger trigger=(CronTrigger)scheduler.getTrigger(TriggerKey.triggerKey(original.getName()+"_trigger"));
        assertThat(trigger.getCronExpression()).isEqualTo(original.getCronExpression());
    }
    @Test void aRenameCannotReplaceAnotherTasksExistingSchedulerJob() throws Exception {
        TaskDO original=task("source-archive","active");existing(original);
        TaskDO other=task("other-task","active");other.setId(2L);existing(other);
        TaskDO changed=task("other-task","active");changed.setCronExpression("0 0 4 * * ?");
        assertThatThrownBy(()->service.updateTask(1L,changed)).isInstanceOf(RuntimeException.class);
        assertThat(scheduler.checkExists(JobKey.jobKey("source-archive"))).isTrue();
        CronTrigger trigger=(CronTrigger)scheduler.getTrigger(TriggerKey.triggerKey("other-task_trigger"));
        assertThat(trigger.getCronExpression()).isEqualTo(other.getCronExpression());
    }
    @Test void aSchedulerNameCollisionDuringRegistrationCannotDeleteTheOtherJob() throws Exception {
        TaskDO original=task("source-archive","active");existing(original);
        scheduler=spy(scheduler);service=spy(new TaskServiceImpl(scheduler));
        doReturn(original).when(service).getById(1L);
        doReturn(0L).when(service).count(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        doReturn(true).when(service).updateById(any(TaskDO.class));
        doAnswer(invocation -> {
            scheduler.scheduleJob(JobBuilder.newJob(CountingJob.class).withIdentity("new-archive").build(),
                Set.of(TriggerBuilder.newTrigger().withIdentity("other_trigger")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 5 * * ?")).build()), false);
            throw new ObjectAlreadyExistsException("fixture concurrent name collision");
        }).when(scheduler).scheduleJob(any(JobDetail.class),any(Trigger.class));
        assertThatThrownBy(()->service.updateTask(1L,task("new-archive","active"))).isInstanceOf(RuntimeException.class);
        assertThat(scheduler.checkExists(JobKey.jobKey("source-archive"))).isTrue();
        assertThat(scheduler.checkExists(JobKey.jobKey("new-archive"))).isTrue();
        assertThat(scheduler.checkExists(TriggerKey.triggerKey("other_trigger"))).isTrue();
    }
    @Test void stoppingThenStartingDoesNotDuplicateTheTrigger() throws Exception {
        TaskDO task=task("sampleTask","active");existing(task);
        doReturn(task).when(service).getById("1");
        service.stopTask("1");service.startTask("1");service.startTask("1");
        assertThat(scheduler.getTriggersOfJob(JobKey.jobKey(task.getName()))).hasSize(1);
        assertThat(scheduler.getTriggerState(TriggerKey.triggerKey(task.getName()+"_trigger"))).isEqualTo(Trigger.TriggerState.NORMAL);
    }
}
