package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.support.AgentDiagnostics;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 本节点执行控制；取消标志持久化，其他节点通过心跳轮询接收。 */
@Component
public class AgentRunControl implements AutoCloseable {
    private final IAgentPersistenceService store;
    private final AgentProperties properties;
    private final Semaphore capacity;
    private final Map<String, Registration> running = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitor =
            Executors.newSingleThreadScheduledExecutor(
                    task -> {
                        Thread thread = new Thread(task, "agent-run-control");
                        thread.setDaemon(true);
                        return thread;
                    });

    public AgentRunControl(IAgentPersistenceService store, AgentProperties properties) {
        this.store = store;
        this.properties = properties;
        capacity = new Semaphore(properties.getMaxConcurrentRuns());
    }

    public void acquire() {
        if (!capacity.tryAcquire())
            throw new IceeBootException(429, ResponseMessage.Agent.EXECUTION_CAPACITY_EXCEEDED);
    }

    public void release() {
        capacity.release();
    }

    public Registration register(String id) {
        Registration entry = new Registration(id);
        running.put(id, entry);
        entry.poll =
                monitor.scheduleWithFixedDelay(
                        () -> {
                            try {
                                if (store.cancellationRequested(id)) entry.cancel();
                                else store.heartbeat(id);
                            } catch (RuntimeException error) {
                                AgentDiagnostics.failure(
                                        id, "CONTROL", AgentDiagnostics.traceId(), error);
                            }
                        },
                        0,
                        1,
                        TimeUnit.SECONDS);
        return entry;
    }

    public void cancel(String id) {
        if (!store.requestCancellation(id))
            throw new IceeBootException(409, ResponseMessage.Agent.CANCEL_NOT_RUNNING);
        Registration entry = running.get(id);
        if (entry != null) entry.cancel();
    }

    public int recoverStale() {
        return store.interruptStale(Math.max(30, properties.getExecutionTimeoutSeconds() + 30L));
    }

    @Override
    @PreDestroy
    public void close() {
        running.values().forEach(Registration::cancel);
        monitor.shutdownNow();
    }

    public final class Registration implements AutoCloseable {
        private final String id;
        private final Sinks.Empty<Void> cancellation = Sinks.empty();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private ScheduledFuture<?> poll;
        private final java.util.concurrent.atomic.AtomicReference<Runnable> stop =
                new java.util.concurrent.atomic.AtomicReference<>();

        private Registration(String id) {
            this.id = id;
        }

        private void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                Runnable action = stop.get();
                try {
                    if (action != null) action.run();
                } finally {
                    cancellation.tryEmitEmpty();
                }
            }
        }

        public void bind(Runnable action) {
            stop.set(action);
            if (cancelled.get()) action.run();
        }

        public boolean cancelled() {
            return cancelled.get();
        }

        public void check() {
            if (cancelled())
                throw new IceeBootException(409, ResponseMessage.Agent.EXECUTION_CANCELLED);
        }

        public <T> Mono<T> apply(Mono<T> task) {
            return task.takeUntilOther(cancellation.asMono());
        }

        @Override
        public void close() {
            if (poll != null) poll.cancel(false);
            running.remove(id, this);
        }
    }
}
