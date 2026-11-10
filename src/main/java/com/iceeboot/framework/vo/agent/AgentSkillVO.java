package com.iceeboot.framework.vo.agent;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 技能响应视图。 */
@Data
@Schema(description = "技能响应视图")
public class AgentSkillVO {
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "技能正文")
    private String content;

    @Schema(description = "技能资源内容")
    private Map<String, String> resources;

    @Schema(description = "绑定工具 ID")
    private List<Long> toolIds;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
