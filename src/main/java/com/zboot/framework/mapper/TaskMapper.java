package com.zboot.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.TaskDO;
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