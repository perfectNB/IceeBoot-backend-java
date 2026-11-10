package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.util.Map;

/** 智能体运行时Generation，仅供服务内部与发布快照使用。 */
public record Generation(
        @DecimalMin("0") @DecimalMax("2") Double temperature,
        @Min(1) @Max(1048576) Integer maxTokens,
        Boolean thinking,
        String reasoningEffort,
        @Min(1024) Integer thinkingBudget,
        Map<String, Object> extra) {}
