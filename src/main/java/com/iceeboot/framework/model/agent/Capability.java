package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** 智能体运行时Capability，仅供服务内部与发布快照使用。 */
public record Capability(
        Long id,
        @NotNull CapabilityKind kind,
        @NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}") String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        boolean enabled,
        SkillConfig skill,
        ToolConfig tool,
        McpConfig mcp,
        @Valid SandboxConfig sandbox) {}
