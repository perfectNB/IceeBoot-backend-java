package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

/** 智能体运行时AgentSpec，仅供服务内部与发布快照使用。 */
public record AgentSpec(
        Long id,
        @NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}") String code,
        @NotBlank @Size(max = 120) String name,
        String description,
        String avatar,
        @NotBlank @Size(max = 100000) String prompt,
        @NotNull Long modelConfigId,
        @Min(1) @Max(100) int maxRounds,
        boolean enabled,
        boolean sandboxEnabled,
        Long sandboxTemplateId,
        boolean planMode,
        @NotNull OutputFormat outputFormat,
        Map<String, Object> outputSchema,
        @Valid Generation generation,
        List<Long> skillIds,
        List<Long> toolIds,
        List<Long> mcpIds,
        List<Long> knowledgeIds,
        List<Long> childAgentIds,
        Map<String, String> environment) {}
