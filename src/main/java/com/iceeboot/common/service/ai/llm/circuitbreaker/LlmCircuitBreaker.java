package com.iceeboot.common.service.ai.llm.circuitbreaker;

import com.iceeboot.common.circuitbreaker.SimpleCircuitBreaker;

/**
 * LLM 调用熔断器。
 *
 * <p>连续失败达到阈值后进入 OPEN 状态，快速拒绝后续请求；
 * 等待冷却时间后进入 HALF_OPEN，允许一次探测请求；
 * 探测成功恢复 CLOSED，失败重新进入 OPEN。</p>
 */
public class LlmCircuitBreaker extends SimpleCircuitBreaker {

    public LlmCircuitBreaker(String name, int failureThreshold, long cooldownMillis) {
        super("LLM", name, failureThreshold, cooldownMillis);
    }
}
