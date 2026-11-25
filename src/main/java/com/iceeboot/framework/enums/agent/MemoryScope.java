package com.iceeboot.framework.enums.agent;

public enum MemoryScope {
    AGENT_USER, USER, AGENT;

    public long policyAgent(long agentId) {
        return this == USER ? 0 : agentId;
    }

    public String owner(String userId) {
        return this == AGENT ? "" : userId;
    }
}
