package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** AgentSandboxConfigDTO请求参数。 */
@Data
@Schema(description = "AgentSandboxConfigDTO")
public class AgentSandboxConfigDTO {
    @Schema(description = "E2B 模板标识")
    private String templateId;

    @Schema(description = "沙箱超时秒数")
    @Min(60)
    @Max(3600)
    private Integer timeoutSeconds;
}
