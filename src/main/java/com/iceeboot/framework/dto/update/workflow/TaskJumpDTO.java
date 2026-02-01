package com.iceeboot.framework.dto.update.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务跳转请求")
public class TaskJumpDTO {
    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "目标节点ID")
    private String targetActivityId;
}
