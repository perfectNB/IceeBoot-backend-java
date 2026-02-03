package com.iceeboot.framework.dto.create.workflow;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.constant.SystemConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "流程启动请求")
public class ProcessStartDTO {
    @Schema(description = "流程定义Key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessage.WorkflowMessage.PROCESS_DEFINITION_KEY_EMPTY)
    private String processDefinitionKey;

    @Schema(description = "业务Key")
    private String businessKey;

    @Schema(description = "流程实例名称")
    private String processInstanceName;

    @Schema(description = "流程变量")
    private Map<String, Object> variables;
}
