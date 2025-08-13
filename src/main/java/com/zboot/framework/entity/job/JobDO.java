/**
 * 任务作业实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.job;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务作业实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_job")
@Schema(description = "任务作业信息")
public class JobDO extends ZbootDO {

    /**
     * 作业名称
     */
    @Schema(description = "作业名称")
    @TableField("job_name")
    private String jobName;

    /**
     * 作业描述
     */
    @Schema(description = "作业描述")
    @TableField("description")
    private String description;

    /**
     * 作业类型
     */
    @Schema(description = "作业类型")
    @TableField("job_type")
    private String jobType;

    /**
     * 作业状态（0-待执行，1-执行中，2-已完成，3-失败，4-已取消）
     */
    @Schema(description = "作业状态")
    @TableField("status")
    private Integer status;

    /**
     * 优先级（1-低，2-中，3-高，4-紧急）
     */
    @Schema(description = "优先级")
    @TableField("priority")
    private Integer priority;

    /**
     * 计划开始时间
     */
    @Schema(description = "计划开始时间")
    @TableField("scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    /**
     * 计划结束时间
     */
    @Schema(description = "计划结束时间")
    @TableField("scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    /**
     * 实际开始时间
     */
    @Schema(description = "实际开始时间")
    @TableField("actual_start_time")
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间
     */
    @Schema(description = "实际结束时间")
    @TableField("actual_end_time")
    private LocalDateTime actualEndTime;

    /**
     * 执行器类名
     */
    @Schema(description = "执行器类名")
    @TableField("executor_class")
    private String executorClass;

    /**
     * 执行参数（JSON格式）
     */
    @Schema(description = "执行参数")
    @TableField("parameters")
    private String parameters;

    /**
     * Cron表达式（定时任务）
     */
    @Schema(description = "Cron表达式")
    @TableField("cron_expression")
    private String cronExpression;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @Schema(description = "最大重试次数")
    @TableField("max_retry_count")
    private Integer maxRetryCount;

    /**
     * 执行结果
     */
    @Schema(description = "执行结果")
    @TableField("result")
    private String result;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    @TableField("error_message")
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    @Schema(description = "执行耗时")
    @TableField("execution_time")
    private Long executionTime;

    /**
     * 负责人
     */
    @Schema(description = "负责人")
    @TableField("assignee")
    private String assignee;

    /**
     * 作业组
     */
    @Schema(description = "作业组")
    @TableField("job_group")
    private String jobGroup;

    /**
     * 是否启用（0-禁用，1-启用）
     */
    @Schema(description = "是否启用")
    @TableField("enabled")
    private Integer enabled;

    /**
     * 下次执行时间
     */
    @Schema(description = "下次执行时间")
    @TableField("next_execution_time")
    private LocalDateTime nextExecutionTime;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息")
    @TableField("remarks")
    private String remarks;
}