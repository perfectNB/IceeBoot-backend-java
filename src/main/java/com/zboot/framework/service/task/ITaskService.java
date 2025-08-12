package com.zboot.framework.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.entity.TaskDO;
import com.zboot.framework.query.system.TaskQuery;
import com.zboot.common.core.domain.PageResult;

/**
 * 定时任务服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
public interface ITaskService extends IService<TaskDO> {

    /**
     * 分页查询任务列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<TaskDO> getTaskPage(TaskQuery query);

    /**
     * 创建任务
     * 
     * @param task 任务信息
     * @return 任务信息
     */
    TaskDO createTask(TaskDO task);

    /**
     * 更新任务
     * 
     * @param id 任务ID
     * @param task 任务信息
     * @return 任务信息
     */
    TaskDO updateTask(String id, TaskDO task);

    /**
     * 删除任务
     * 
     * @param id 任务ID
     */
    void deleteTask(String id);

    /**
     * 启动任务
     * 
     * @param id 任务ID
     */
    void startTask(String id);

    /**
     * 停止任务
     * 
     * @param id 任务ID
     */
    void stopTask(String id);
}