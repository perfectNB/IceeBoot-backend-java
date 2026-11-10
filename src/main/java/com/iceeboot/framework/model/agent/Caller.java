package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

/** 智能体运行时Caller，仅供服务内部与发布快照使用。 */
public record Caller(String userId, String accessToken) {}
