package com.zboot.framework.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.exception.ZbootException;
import com.zboot.framework.dto.task.TaskDTO;
import com.zboot.framework.entity.task.TaskDO;
import com.zboot.framework.mapper.TaskMapper;
import com.zboot.framework.query.system.TaskQuery;
import com.zboot.framework.service.task.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 定时任务服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, TaskDO> implements ITaskService {

    private final Scheduler scheduler;

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
    public TaskDO createTask(TaskDO task) {
        try {
            // 保存到数据库
            this.save(task);
            
            // 创建Quartz任务
            if (task.getStatus().equals(SystemConstants.Status.INACTIVE)) {
                createQuartzJob(task);
            }
            
            return task;
        } catch (Exception e) {
            log.error("创建定时任务失败: {}", e.getMessage(), e);
            throw new ZbootException("创建定时任务失败", e);
        }
    }

    @Override
    public boolean createTask(TaskDTO taskDTO) {
        try {
            TaskDO task = new TaskDO();
            BeanUtils.copyProperties(taskDTO, task);
            
            // 保存到数据库
            this.save(task);
            
            // 创建Quartz任务
            if (task.getStatus() != null && task.getStatus().equals(SystemConstants.Status.ACTIVE)) {
                createQuartzJob(task);
            }
            
            return true;
        } catch (Exception e) {
            log.error("创建定时任务失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public TaskDO updateTask(Long id, TaskDO task) {
        try {
            task.setId(id);
            // 更新数据库
            this.updateById(task);
            
            // 删除旧任务
            deleteQuartzJob(task.getName());
            // 创建新任务
            if (task.getStatus().equals(SystemConstants.Status.INACTIVE)) {
                createQuartzJob(task);
            }
            
            return task;
        } catch (Exception e) {
            log.error("更新定时任务失败: {}", e.getMessage(), e);
            throw new ZbootException("更新定时任务失败", e);
        }
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
            throw new ZbootException("删除定时任务失败", e);
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
                scheduler.resumeJob(jobKey);
                
                // 更新状态
                task.setStatus(SystemConstants.Status.ACTIVE); // 启用
                this.updateById(task);
                
                log.info("启动定时任务成功: {}", task.getName());
            }
        } catch (Exception e) {
            log.error("启动定时任务失败: {}", e.getMessage(), e);
            throw new ZbootException("启动定时任务失败", e);
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
            throw new ZbootException("停止定时任务失败", e);
        }
    }

    /**
     * 创建Quartz任务
     */
    private void createQuartzJob(TaskDO task) throws Exception {
        Class<?> jobClass = Class.forName(task.getClassName());
        
        JobDetail jobDetail = JobBuilder.newJob((Class<? extends Job>) jobClass)
                .withIdentity(task.getName())
                .withDescription(task.getDescription())
                .build();
        
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(task.getName() + "_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                .build();
        
        scheduler.scheduleJob(jobDetail, trigger);
        
        // 根据状态决定是否暂停
        if (task.getStatus().equals(SystemConstants.Status.INACTIVE)) {
            scheduler.pauseJob(JobKey.jobKey(task.getName()));
        }
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