package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** AgentModelInvokeDTO请求参数。 */
@Data
@Schema(description = "AgentModelInvokeDTO")
public class AgentModelInvokeDTO {
    @Schema(description = "模型连接 ID")
    @NotNull
    private Long modelConfigId;

    @Schema(description = "调用输入")
    @NotBlank
    @Size(max = 100000)
    private String input;

    @Schema(description = "系统提示词")
    private String systemPrompt;
}
