package com.zboot.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务实体
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_task")
public class TaskDO {

    /**
     * 任务ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 任务名称
     */
    @TableField("name")
    private String name;

    /**
     * 任务类名
     */
    @TableField("class_name")
    private String className;

    /**
     * Cron表达式
     */
    @TableField("cron_expression")
    private String cronExpression;

    /**
     * 任务状态：0-停止，1-运行
     */
    @TableField("status")
    private Integer status;

    /**
     * 任务描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}