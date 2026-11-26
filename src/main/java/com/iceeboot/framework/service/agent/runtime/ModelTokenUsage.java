package com.iceeboot.framework.service.agent.runtime;

import io.agentscope.core.model.ChatUsage;

/** SDK 把缓存字段缺失和零命中都映射为 0，只累计可以确认的命中数。 */
public final class ModelTokenUsage {
    private ModelTokenUsage() {}

    public static Long confirmedWriteTokens(ChatUsage usage) {
        if (usage == null || usage.getCacheCreationTokens() <= 0) return null;
        return (long) usage.getCacheCreationTokens();
    }

    public static Long confirmedCachedTokens(ChatUsage usage) {
        if (usage == null || usage.getCachedTokens() <= 0) return null;
        return (long) usage.getCachedTokens();
    }
}
