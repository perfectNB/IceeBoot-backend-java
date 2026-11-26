package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** AgentVO响应视图，敏感字段由服务脱敏。 */
@Data
@Schema(description = "AgentVO")
public class AgentVO {
    @Schema(description = "智能体草稿")
    private AgentDefinitionVO draft;

    @Schema(description = "当前发布版本 ID")
    private String activeReleaseId;

    @Schema(description = "当前发布版本号")
    private int activeVersion;

    @Schema(description = "调用次数")
    private long callCount;

    private Long totalInputTokens;

    private Long totalOutputTokens;

    private Long totalCachedTokens;
}
