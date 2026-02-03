package com.iceeboot.framework.dto.update.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "任务完成请求")
public class TaskCompleteDTO {
    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务变量")
    private Map<String, Object> variables;

    @Schema(description = "审批意见")
    private String comment;
}
