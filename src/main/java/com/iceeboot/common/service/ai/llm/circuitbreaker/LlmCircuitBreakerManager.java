package com.iceeboot.common.service.ai.llm.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM 熔断器管理器，按 provider 维度管理熔断器实例。
 *
 * <p>熔断参数：
 * - 连续失败 3 次触发熔断
 * - 冷却 30 秒后进入半开探测
 * </p>
 */
@Slf4j
@Component
public class LlmCircuitBreakerManager {

    private static final int DEFAULT_FAILURE_THRESHOLD = 3;
    private static final long DEFAULT_COOLDOWN_MILLIS = 30_000;

    private final Map<String, LlmCircuitBreaker> breakers = new ConcurrentHashMap<>();

    public LlmCircuitBreaker get(String provider) {
        return breakers.computeIfAbsent(provider.trim().toLowerCase(java.util.Locale.ROOT),
                k -> new LlmCircuitBreaker(k, DEFAULT_FAILURE_THRESHOLD, DEFAULT_COOLDOWN_MILLIS));
    }
}
