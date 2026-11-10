package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 技能请求参数。 */
@Data
@Schema(description = "技能请求参数")
public class AgentSkillDTO {
    @Schema(description = "编码")
    @NotBlank
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}")
    private String code;

    @Schema(description = "名称")
    @NotBlank
    @Size(max = 120)
    private String name;

    @Schema(description = "说明")
    @Size(max = 2000)
    private String description;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "技能正文")
    @NotBlank
    @Size(max = 100000)
    private String content;

    @Schema(description = "技能资源内容")
    private Map<String, String> resources;

    @Schema(description = "绑定工具 ID")
    private List<Long> toolIds;
}
