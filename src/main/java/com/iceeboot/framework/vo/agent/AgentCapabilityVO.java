package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 能力注册响应视图。 */
@Data
@Schema(description = "能力注册响应视图")
public class AgentCapabilityVO {
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "类型")
    private CapabilityKind kind;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "技能配置")
    private AgentSkillConfigVO skill;

    @Schema(description = "工具配置")
    private AgentToolConfigVO tool;

    @Schema(description = "MCP 配置")
    private AgentMcpConfigVO mcp;

    @Schema(description = "沙箱配置")
    private AgentSandboxConfigVO sandbox;
}
