package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** AgentSandboxConfigVO响应视图，敏感字段由服务脱敏。 */
@Data
@Schema(description = "AgentSandboxConfigVO")
public class AgentSandboxConfigVO {
    @Schema(description = "E2B 模板标识")
    private String templateId;

    @Schema(description = "沙箱超时秒数")
    private Integer timeoutSeconds;
}
