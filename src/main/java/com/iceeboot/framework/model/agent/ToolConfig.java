package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.util.Map;

/** 智能体运行时ToolConfig，仅供服务内部与发布快照使用。 */
public record ToolConfig(
        ToolKind kind,
        String url,
        String method,
        Map<String, String> headers,
        Map<String, Object> inputSchema,
        String script,
        boolean projectApi,
        String permissionCode) {}
