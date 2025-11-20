package com.iceeboot.framework.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务实体
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_task")
@Schema(description = "定时任务")
public class TaskDO extends IceeBootDO {


    @Schema(description = "任务名称")
    @TableField("name")
    private String name;


    @Schema(description = "任务类名")
    @TableField("class_name")
    private String className;


    @Schema(description = "Cron表达式")
    @TableField("cron_expression")
    private String cronExpression;


    @Schema(description = "任务状态：ininactive-停止，inactive-运行")
    @TableField("status")
    private String status;


    @Schema(description = "任务描述")
    @TableField("description")
    private String description;


}