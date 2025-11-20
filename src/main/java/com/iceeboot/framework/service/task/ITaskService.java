package com.iceeboot.framework.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.task.TaskDTO;
import com.iceeboot.framework.entity.task.TaskDO;
import com.iceeboot.framework.query.system.TaskQuery;

import java.util.List;

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
     * 创建任务
     * 
     * @param taskDTO 任务数据传输对象
     * @return 创建是否成功
     */
    boolean createTask(TaskDTO taskDTO);

    /**
     * 更新任务
     * 
     * @param id 任务ID
     * @param task 任务信息
     * @return 任务信息
     */
    TaskDO updateTask(Long id, TaskDO task);

    /**
     * 删除任务
     * 
     * @param id 任务ID
     */
    void deleteTask(Long id);

    /**
     * 批量删除任务
     * 
     * @param ids 任务ID列表
     * @return 删除是否成功
     */
    boolean deleteTasks(List<Long> ids);

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