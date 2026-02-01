package com.iceeboot.framework.query.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "流程定义查询条件")
public class ProcessDefinitionQuery {
    @Schema(description = "仅返回最新版本")
    private Boolean latestOnly;
}
