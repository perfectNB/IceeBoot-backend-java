package com.iceeboot.framework.service.agent.runtime;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.session.SessionLog;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.concurrent.atomic.AtomicReference;

/** 只释放本次模型请求实际持有的会话租约，避免取消后遗留 writer；不会接管其他调用的租约。 */
public final class AgentSessionLease implements AutoCloseable {
    private final SessionLog log;
    private final AtomicReference<SessionLog.Writer> owned = new AtomicReference<>();

    public AgentSessionLease(HarnessAgent agent, RuntimeContext context) {
        log = agent.sessionLog(context);
    }

    /** 模型订阅发生在 SDK 成功取得 writer 之后，此时记录 owner 与 epoch。 */
    public void capture() {
        if (owned.get() != null) return;
        SessionLog.Head head = log.head();
        if (head.owner() != null && !head.owner().isBlank())
            owned.compareAndSet(null, new SessionLog.Writer(head.owner(), head.epoch()));
    }

    @Override
    public void close() {
        SessionLog.Writer writer = owned.getAndSet(null);
        if (writer == null) return;
        // Reactor 的取消信号先于 SDK 异步 usingWhen 清理完成；在释放会话互斥之前等其落盘。
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        SessionLog.Head head = log.head();
        while (writer.owner().equals(head.owner())
                && writer.epoch() == head.epoch()
                && System.nanoTime() < deadline
                && !Thread.currentThread().isInterrupted()) {
            java.util.concurrent.locks.LockSupport.parkNanos(
                    java.time.Duration.ofMillis(10).toNanos());
            head = log.head();
        }
        if (writer.owner().equals(head.owner()) && writer.epoch() == head.epoch())
            log.release(writer);
    }
}
