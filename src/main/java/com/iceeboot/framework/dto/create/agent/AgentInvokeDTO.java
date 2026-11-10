package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** AgentInvokeDTO请求参数。 */
@Data
@Schema(description = "AgentInvokeDTO")
public class AgentInvokeDTO {
    @Schema(description = "调用输入")
    @NotBlank
    @Size(max = 100000)
    private String input;

    @Schema(description = "会话标识")
    @Size(max = 100)
    private String sessionId;
}
