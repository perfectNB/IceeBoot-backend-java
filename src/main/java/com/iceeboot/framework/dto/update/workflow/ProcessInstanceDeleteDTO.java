package com.iceeboot.framework.dto.update.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "流程实例删除请求")
public class ProcessInstanceDeleteDTO {
    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "删除原因")
    private String deleteReason;
}
