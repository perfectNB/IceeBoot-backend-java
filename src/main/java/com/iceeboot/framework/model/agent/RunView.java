package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** 智能体运行时RunView，仅供服务内部与发布快照使用。 */
public record RunView(
        String id,
        String rootRunId,
        String parentRunId,
        Long agentId,
        String agentCode,
        int version,
        String userId,
        String status,
        String input,
        String output,
        String error,
        long inputTokens,
        long outputTokens,
        Long cachedTokens,
        boolean usageKnown,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String errorCode,
        String traceId,
        boolean cancelRequested) {}
