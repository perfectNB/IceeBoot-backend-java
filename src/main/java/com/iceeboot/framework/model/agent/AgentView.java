package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

/** 智能体运行时AgentView，仅供服务内部与发布快照使用。 */
public record AgentView(
        AgentSpec draft, String activeReleaseId, int activeVersion, long callCount) {}
