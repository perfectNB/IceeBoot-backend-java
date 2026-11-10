package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

/** 智能体运行时McpConfig，仅供服务内部与发布快照使用。 */
public record McpConfig(String url, Map<String, String> headers, List<String> allowedTools) {}
