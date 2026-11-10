package com.iceeboot.common.circuitbreaker;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SimpleCircuitBreakerTest {
    @Test void cooldownRejectsRequestsUntilProbeIsAllowed() {
        var breaker = new SimpleCircuitBreaker("test", "fixture", 2, 60_000);
        breaker.onFailure();
        assertThat(breaker.allowRequest()).isTrue();
        breaker.onFailure();
        assertThat(breaker.getState()).isEqualTo(SimpleCircuitBreaker.State.OPEN);
        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test void halfOpenAllowsOneProbeAndSuccessRestoresTraffic() {
        var breaker = new SimpleCircuitBreaker("test", "fixture", 1, 0);
        breaker.onFailure();
        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.allowRequest()).isFalse();
        breaker.onSuccess();
        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.getState()).isEqualTo(SimpleCircuitBreaker.State.CLOSED);
    }

    @Test void failedProbeReopensCircuit() {
        var breaker = new SimpleCircuitBreaker("test", "fixture", 1, 0);
        breaker.onFailure();
        assertThat(breaker.allowRequest()).isTrue();
        breaker.onFailure();
        assertThat(breaker.getState()).isEqualTo(SimpleCircuitBreaker.State.OPEN);
    }
}
