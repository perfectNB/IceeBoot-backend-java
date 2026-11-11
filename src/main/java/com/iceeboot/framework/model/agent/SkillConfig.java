package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

/** 智能体运行时SkillConfig，仅供服务内部与发布快照使用。 */
public record SkillConfig(String content, Map<String, String> resources, List<Long> toolIds) {
    public static final int MAX_RESOURCES = 500;
    public static final int MAX_RESOURCE_CHARACTERS = 1_000_000;
    public static final int MAX_TOTAL_RESOURCE_BYTES = 20 * 1024 * 1024;
}
