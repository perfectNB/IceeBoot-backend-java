/**
 * 定时任务实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.task;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zboot.common.core.domain.ZbootDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_task")
public class TaskDO extends ZbootDO {

    /**
     * 任务名称
     */
    @TableField("name")
    private String name;

    /**
     * 类名
     */
    @TableField("class_name")
    private String className;

    /**
     * Cron表达式
     */
    @TableField("cron_expression")
    private String cronExpression;

    /**
     * 任务状态（running-运行中，stopped-已停止，paused-已暂停）
     */
    @TableField("status")
    private String status;

    /**
     * 任务描述
     */
    @TableField("description")
    private String description;

    /**
     * 最后执行时间
     */
    @TableField("last_execute_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecuteTime;

    /**
     * 下次执行时间
     */
    @TableField("next_execute_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecuteTime;
}