package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

/** 智能体运行时ModelInvokeRequest，仅供服务内部与发布快照使用。 */
public record ModelInvokeRequest(
        @NotNull Long modelConfigId,
        @NotBlank @Size(max = 100000) String input,
        String systemPrompt) {}
