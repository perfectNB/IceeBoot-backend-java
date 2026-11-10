package com.iceeboot.framework.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.task.TaskDTO;
import com.iceeboot.framework.entity.task.TaskDO;
import com.iceeboot.framework.mapper.TaskMapper;
import com.iceeboot.framework.query.system.TaskQuery;
import com.iceeboot.framework.service.task.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.quartz.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * 定时任务服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, TaskDO> implements ITaskService, ApplicationRunner {

    private final Scheduler scheduler;


    @Override
    public void run(ApplicationArguments args) {
        List<TaskDO> activeTasks = this.list(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getStatus, SystemConstants.Status.ACTIVE)
                .eq(TaskDO::getDelFlag, 0));
        for (TaskDO task : activeTasks) {
            try {
                if (!scheduler.checkExists(JobKey.jobKey(task.getName()))) {
                    createQuartzJob(task);
                }
            } catch (Exception exception) {
                log.error("恢复定时任务失败: name={}, className={}",
                        task.getName(), task.getClassName(), exception);
            }
        }
        log.info("启用的定时任务恢复完成: count={}", activeTasks.size());
    }

    @Override
    public PageResult<TaskDO> getTaskPage(TaskQuery query) {
        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(TaskDO::getName, query.getName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(TaskDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(TaskDO::getCreateTime);
        
        Page<TaskDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TaskDO> pageResult = this.page(page, wrapper);
        
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDO createTask(TaskDO task) {
        try {
            if (task.getStatus() == null) task.setStatus(SystemConstants.Status.INACTIVE);
            PreparedJob prepared = prepareQuartzJob(task);
            ensureAvailableName(task.getName(), null);
            if (!this.save(task)) throw new IllegalStateException("任务保存失败");
            if (SystemConstants.Status.ACTIVE.equals(task.getStatus())) {
                scheduler.scheduleJob(prepared.detail(), prepared.trigger());
                String createdName = task.getName();
                onRollback(() -> deleteQuartzJob(createdName));
            }
            return task;
        } catch (Exception e) {
            log.error("创建定时任务失败", e);
            throw new IceeBootException("创建定时任务失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTask(TaskDTO taskDTO) {
        TaskDO task = new TaskDO();
        BeanUtils.copyProperties(taskDTO, task);
        // Let failures propagate so Spring rolls back the saved row as well.
        createTask(task);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDO updateTask(Long id, TaskDO task) {
        try {
            TaskDO previous = this.getById(id);
            if (previous == null) throw new IllegalArgumentException("任务不存在");
            task.setId(id);
            if (task.getName() == null) task.setName(previous.getName());
            if (task.getClassName() == null) task.setClassName(previous.getClassName());
            if (task.getCronExpression() == null) task.setCronExpression(previous.getCronExpression());
            if (task.getStatus() == null) task.setStatus(previous.getStatus());
            PreparedJob prepared = prepareQuartzJob(task); // Validate before touching DB or the working job.
            if (!previous.getName().equals(task.getName())) ensureAvailableName(task.getName(), id);
            SchedulerSnapshot snapshot = snapshot(previous.getName());
            if (!this.updateById(task)) throw new IllegalStateException("任务更新失败");
            try {
                deleteQuartzJob(previous.getName());
                if (SystemConstants.Status.ACTIVE.equals(task.getStatus())) {
                    scheduler.scheduleJob(prepared.detail(), prepared.trigger());
                }
            } catch (Exception failure) {
                // scheduleJob is atomic; a rejected new name may now belong to another job.
                // Restore only our previous key instead of deleting the rejected target name.
                try { restore(snapshot, previous.getName()); }
                catch (Exception restoreFailure) { failure.addSuppressed(restoreFailure); }
                throw failure;
            }
            String changedName = task.getName();
            onRollback(() -> restore(snapshot, changedName));
            return task;
        } catch (Exception e) {
            log.error("更新定时任务失败", e);
            throw new IceeBootException("更新定时任务失败", e);
        }
    }

    private void ensureAvailableName(String name, Long exceptId) throws SchedulerException {
        var query = new LambdaQueryWrapper<TaskDO>().eq(TaskDO::getName, name);
        if (exceptId != null) query.ne(TaskDO::getId, exceptId);
        if (this.count(query) > 0 || scheduler.checkExists(JobKey.jobKey(name))
                || scheduler.checkExists(TriggerKey.triggerKey(name + "_trigger"))) {
            throw new IllegalArgumentException("任务名称已存在");
        }
    }

    private record SchedulerSnapshot(JobDetail detail, List<Trigger> triggers, List<TriggerKey> paused) {}
    private SchedulerSnapshot snapshot(String name) throws SchedulerException {
        JobKey key = JobKey.jobKey(name);
        List<Trigger> triggers = new ArrayList<>(scheduler.getTriggersOfJob(key));
        List<TriggerKey> paused = new ArrayList<>();
        for (Trigger trigger : triggers) {
            if (scheduler.getTriggerState(trigger.getKey()) == Trigger.TriggerState.PAUSED) paused.add(trigger.getKey());
        }
        return new SchedulerSnapshot(scheduler.getJobDetail(key), triggers, paused);
    }
    private void restore(SchedulerSnapshot snapshot, String changedName) throws Exception {
        deleteQuartzJob(changedName);
        if (snapshot.detail() != null) {
            deleteQuartzJob(snapshot.detail().getKey().getName());
            scheduler.scheduleJob(snapshot.detail(), new HashSet<>(snapshot.triggers()), false);
            for (TriggerKey key : snapshot.paused()) scheduler.pauseTrigger(key);
        }
    }
    @FunctionalInterface private interface SchedulerRollback { void run() throws Exception; }
    private void onRollback(SchedulerRollback action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try { action.run(); }
                    catch (Exception failure) { log.error("数据库回滚后恢复定时任务失败", failure); }
                }
            }
        });
    }

    @Override
    public void deleteTask(Long id) {
        try {
            TaskDO task = this.getById(id);
            if (task != null) {
                // 删除Quartz任务
                deleteQuartzJob(task.getName());
                // 删除数据库记录
                this.removeById(id);
            }
        } catch (Exception e) {
            log.error("删除定时任务失败: {}", e.getMessage(), e);
            throw new IceeBootException("删除定时任务失败", e);
        }
    }

    @Override
    public boolean deleteTasks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        try {
            for (Long id : ids) {
                TaskDO task = this.getById(id);
                if (task != null) {
                    // 删除Quartz任务
                    deleteQuartzJob(task.getName());
                }
            }
            // 批量删除数据库记录
            return this.removeByIds(ids);
        } catch (Exception e) {
            log.error("批量删除定时任务失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void startTask(String id) {
        try {
            TaskDO task = this.getById(id);
            if (task != null) {
                // 启动任务
                JobKey jobKey = JobKey.jobKey(task.getName());
                // A database row may be active while its RAM scheduler job was removed by an edit or restart.
                if (!scheduler.checkExists(jobKey)) {
                    createQuartzJob(task);
                }
                scheduler.resumeJob(jobKey);
                
                // 更新状态
                task.setStatus(SystemConstants.Status.ACTIVE); // 启用
                this.updateById(task);
                
                log.info("启动定时任务成功: {}", task.getName());
            }
        } catch (Exception e) {
            log.error("启动定时任务失败: {}", e.getMessage(), e);
            throw new IceeBootException("启动定时任务失败", e);
        }
    }

    @Override
    public void stopTask(String id) {
        try {
            TaskDO task = this.getById(id);
            if (task != null) {
                // 暂停任务
                JobKey jobKey = JobKey.jobKey(task.getName());
                scheduler.pauseJob(jobKey);
                
                // 更新状态
                task.setStatus(SystemConstants.Status.INACTIVE); // 禁用
                this.updateById(task);
                
                log.info("停止定时任务成功: {}", task.getName());
            }
        } catch (Exception e) {
            log.error("停止定时任务失败: {}", e.getMessage(), e);
            throw new IceeBootException("停止定时任务失败", e);
        }
    }

    /**
     * 创建Quartz任务
     */
    private record PreparedJob(JobDetail detail, CronTrigger trigger) {}
    private PreparedJob prepareQuartzJob(TaskDO task) throws Exception {
        if (!StringUtils.hasText(task.getName())) throw new IllegalArgumentException("任务名称不能为空");
        if (!SystemConstants.Status.ACTIVE.equals(task.getStatus())
                && !SystemConstants.Status.INACTIVE.equals(task.getStatus())) {
            throw new IllegalArgumentException("任务状态无效");
        }
        Class<? extends Job> jobClass = Class.forName(task.getClassName()).asSubclass(Job.class);
        JobDetail detail = JobBuilder.newJob(jobClass).withIdentity(task.getName())
                .withDescription(task.getDescription()).build();
        CronScheduleBuilder schedule = CronScheduleBuilder.cronSchedule(task.getCronExpression());
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(task.getName() + "_trigger")
                .withSchedule(schedule).build();
        return new PreparedJob(detail, trigger);
    }
    private void createQuartzJob(TaskDO task) throws Exception {
        PreparedJob prepared = prepareQuartzJob(task);
        scheduler.scheduleJob(prepared.detail(), prepared.trigger());
        if (SystemConstants.Status.INACTIVE.equals(task.getStatus())) scheduler.pauseJob(JobKey.jobKey(task.getName()));
    }

    /**
     * 删除Quartz任务
     */
    private void deleteQuartzJob(String taskName) throws Exception {
        JobKey jobKey = JobKey.jobKey(taskName);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }
}
