package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 沙箱模板请求参数。 */
@Data
@Schema(description = "沙箱模板请求参数")
public class AgentSandboxTemplateDTO {
    @Schema(description = "编码")
    @NotBlank
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}")
    private String code;

    @Schema(description = "名称")
    @NotBlank
    @Size(max = 120)
    private String name;

    @Schema(description = "说明")
    @Size(max = 2000)
    private String description;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "E2B 模板标识")
    @NotBlank
    private String templateId;

    @Schema(description = "沙箱超时秒数")
    @Min(60)
    @Max(3600)
    private Integer timeoutSeconds;
}
