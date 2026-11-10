package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.util.List;

/** 智能体运行时PublishedAgent，仅供服务内部与发布快照使用。 */
public record PublishedAgent(
        String releaseId,
        int version,
        AgentSpec agent,
        ModelConfig model,
        List<Capability> capabilities,
        List<PublishedAgent> children) {}
