package com.iceeboot.framework.model.agent;

public record BillingSource(Long connectionId, String provider, Long cacheWriteTokens) {
    public static BillingSource of(ModelConfig model, Long written) {
        return new BillingSource(model.id(), model.provider().name(), written);
    }
}
