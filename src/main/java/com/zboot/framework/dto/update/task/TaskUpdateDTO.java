package com.zboot.framework.dto.update.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 定时任务更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "定时任务更新数据传输对象")
public class TaskUpdateDTO {

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务名称不能为空")
    private String name;

    @Schema(description = "任务类名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务类名不能为空")
    private String className;

    @Schema(description = "Cron表达式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    @Schema(description = "任务状态：active-运行，inactive-停止")
    private String status;

    @Schema(description = "任务描述")
    private String description;
}