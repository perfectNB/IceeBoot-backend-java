package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 技能响应视图。 */
@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Schema(description = "技能响应视图")
public class AgentSkillVO extends AgentResourceVO {
    @Schema(description = "技能正文")
    private String content;

    @Schema(description = "技能资源内容")
    private Map<String, String> resources;

    @Schema(description = "绑定工具 ID")
    private List<Long> toolIds;
}
