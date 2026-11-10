package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.util.List;

/** 智能体运行时RunDetail，仅供服务内部与发布快照使用。 */
public record RunDetail(
        List<RunView> agents,
        List<ModelCallView> modelCalls,
        long totalInputTokens,
        long totalOutputTokens,
        boolean usageKnown) {}
