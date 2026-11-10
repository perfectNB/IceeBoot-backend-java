package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** AgentSkillConfigVO响应视图，敏感字段由服务脱敏。 */
@Data
@Schema(description = "AgentSkillConfigVO")
public class AgentSkillConfigVO {
    @Schema(description = "技能正文")
    private String content;

    @Schema(description = "技能资源内容")
    private Map<String, String> resources;

    @Schema(description = "绑定工具 ID")
    private List<Long> toolIds;
}
