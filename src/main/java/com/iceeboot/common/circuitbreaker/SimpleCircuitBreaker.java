package com.iceeboot.common.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import java.time.Instant;

/** 连续失败熔断，冷却期之后只放行一个半开探测请求。 */
@Slf4j
public class SimpleCircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String type;
    private final String name;
    private final int failureThreshold;
    private final long cooldownMillis;
    private State state = State.CLOSED;
    private int failureCount;
    private Instant openedAt;

    public SimpleCircuitBreaker(String type, String name, int failureThreshold, long cooldownMillis) {
        if (failureThreshold < 1 || cooldownMillis < 0) {
            throw new IllegalArgumentException("熔断阈值必须大于零，冷却时间不能为负数");
        }
        this.type = type;
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.cooldownMillis = cooldownMillis;
    }

    public synchronized boolean allowRequest() {
        if (state == State.CLOSED) return true;
        if (state == State.OPEN && !Instant.now().isBefore(openedAt.plusMillis(cooldownMillis))) {
            state = State.HALF_OPEN;
            log.info("{} 熔断器 [{}] 进入半开状态", type, name);
            return true;
        }
        return false;
    }

    public synchronized void onSuccess() {
        state = State.CLOSED;
        failureCount = 0;
    }

    public synchronized void onFailure() {
        if (state == State.HALF_OPEN || (state == State.CLOSED && ++failureCount >= failureThreshold)) {
            openedAt = Instant.now();
            state = State.OPEN;
            log.warn("{} 熔断器 [{}] 已打开，冷却 {}ms", type, name, cooldownMillis);
        }
    }

    public String getName() { return name; }
    public synchronized State getState() { return state; }
}
