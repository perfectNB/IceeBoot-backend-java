package com.iceeboot.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.task.TaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务Mapper
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Mapper
public interface TaskMapper extends BaseMapper<TaskDO> {
}