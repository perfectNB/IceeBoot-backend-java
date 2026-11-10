package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** 智能体运行时ModelConfig，仅供服务内部与发布快照使用。 */
public record ModelConfig(
        Long id,
        @NotBlank @Size(max = 120) String name,
        @NotNull Provider provider,
        @NotNull Protocol protocol,
        @NotBlank @Size(max = 1000) String baseUrl,
        String apiKey,
        @NotBlank @Size(max = 200) String model,
        @Min(1024) Integer contextWindow,
        boolean enabled,
        @Valid Generation generation) {}
