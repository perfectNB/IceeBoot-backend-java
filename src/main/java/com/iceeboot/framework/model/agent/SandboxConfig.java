package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

/** 智能体运行时SandboxConfig，仅供服务内部与发布快照使用。 */
public record SandboxConfig(String templateId, @Min(60) @Max(3600) Integer timeoutSeconds) {}
