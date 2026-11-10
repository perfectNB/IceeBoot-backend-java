package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Data;

/** 能力注册请求参数。 */
@Data
@Schema(description = "能力注册请求参数")
public class AgentCapabilityDTO {
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "类型")
    @NotNull
    private CapabilityKind kind;

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

    @Schema(description = "技能配置")
    @Valid
    private AgentSkillConfigDTO skill;

    @Schema(description = "工具配置")
    @Valid
    private AgentToolConfigDTO tool;

    @Schema(description = "MCP 配置")
    @Valid
    private AgentMcpConfigDTO mcp;

    @Schema(description = "沙箱配置")
    @Valid
    private AgentSandboxConfigDTO sandbox;
}
