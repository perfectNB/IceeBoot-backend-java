package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 智能体发布版本响应视图。 */
@Data
@Schema(description = "智能体发布版本响应视图")
public class AgentReleaseVO {
    @Schema(description = "主键 ID")
    private String id;

    @Schema(description = "版本号")
    private int version;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "创建者")
    private String createdBy;
}
