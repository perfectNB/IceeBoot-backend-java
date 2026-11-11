package com.iceeboot.framework.service.agent.support;

import com.iceeboot.common.exception.IceeBootException;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;

import java.util.*;
import java.util.stream.Collectors;

/** 只记录可定位的类型、阶段与栈位置，禁止把 SDK 原始消息、请求或凭据写入日志。 */
@Slf4j
public final class AgentDiagnostics {
    private AgentDiagnostics() {}

    public static String traceId() {
        String trace = MDC.get("traceId");
        return trace != null && trace.matches("[a-zA-Z0-9_-]{1,64}")
                ? trace
                : UUID.randomUUID().toString();
    }

    public static String failure(String run, String stage, String trace, Throwable error) {
        String code =
                error instanceof IceeBootException e && Integer.valueOf(429).equals(e.getCode())
                        ? "QUOTA_EXCEEDED"
                        : error instanceof IceeBootException && com.iceeboot.common.constant.ResponseMessage.Agent.EXECUTION_MAX_ROUNDS_EXCEEDED.equals(error.getMessage())
                            ? "MAX_ROUNDS_EXCEEDED" : stage + "_FAILED";
        var chain = new ArrayList<String>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = error; t != null && seen.add(t) && chain.size() < 8; t = t.getCause()) {
            String locations =
                    Arrays.stream(t.getStackTrace())
                            .filter(x -> x.getClassName().startsWith("com.iceeboot."))
                            .limit(8)
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining(";"));
            chain.add(t.getClass().getSimpleName() + "@" + locations);
        }
        log.error(
                "agent_failure run={} stage={} trace={} code={} locations={}",
                run,
                stage,
                trace,
                code,
                chain);
        return code;
    }
}
