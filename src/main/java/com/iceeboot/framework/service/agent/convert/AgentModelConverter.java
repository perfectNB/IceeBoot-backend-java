package com.iceeboot.framework.service.agent.convert;

import com.iceeboot.framework.model.agent.*;

/** 运行时模型与数据库主键的转换。 */
public final class AgentModelConverter {
    private AgentModelConverter() {}

    public static ModelConfig withId(ModelConfig m, Long id) {
        return new ModelConfig(
                id,
                m.name(),
                m.provider(),
                m.protocol(),
                m.baseUrl(),
                m.apiKey(),
                m.model(),
                m.contextWindow(),
                m.enabled(),
                m.generation());
    }

    public static Capability withId(Capability c, Long id) {
        return new Capability(
                id,
                c.kind(),
                c.code(),
                c.name(),
                c.description(),
                c.enabled(),
                c.skill(),
                c.tool(),
                c.mcp(),
                c.sandbox());
    }

    public static AgentSpec withId(AgentSpec a, Long id) {
        return new AgentSpec(
                id,
                a.code(),
                a.name(),
                a.description(),
                a.avatar(),
                a.prompt(),
                a.modelConfigId(),
                a.maxRounds(),
                a.enabled(),
                a.sandboxEnabled(),
                a.sandboxTemplateId(),
                a.planMode(),
                a.outputFormat(),
                a.outputSchema(),
                a.generation(),
                a.skillIds(),
                a.toolIds(),
                a.mcpIds(),
                a.knowledgeIds(),
                a.childAgentIds(),
                a.environment());
    }
}
