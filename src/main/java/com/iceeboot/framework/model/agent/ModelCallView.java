package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** 智能体运行时ModelCallView，仅供服务内部与发布快照使用。 */
public record ModelCallView(
        String id,
        String runId,
        String rootRunId,
        String agentCode,
        String model,
        String input,
        String output,
        long inputTokens,
        long outputTokens,
        boolean usageKnown,
        LocalDateTime createdAt) {}
