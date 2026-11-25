package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.model.agent.memory.MemoryData.Rule;

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
        List<PublishedAgent> children, List<Rule> memoryRules) {
    public PublishedAgent(String releaseId, int version, AgentSpec agent, ModelConfig model,
                          List<Capability> capabilities, List<PublishedAgent> children) {
        this(releaseId, version, agent, model, capabilities, children, List.of());
    }
    public PublishedAgent {
        memoryRules = memoryRules == null ? List.of() : List.copyOf(memoryRules);
    }
}
