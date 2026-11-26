package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 模型调用记录响应视图。 */
@Data
@Schema(description = "模型调用记录响应视图")
public class AgentModelCallVO {
    @Schema(description = "主键 ID")
    private String id;

    @Schema(description = "执行记录 ID")
    private String runId;

    @Schema(description = "根执行记录 ID")
    private String rootRunId;

    @Schema(description = "智能体编码")
    private String agentCode;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "调用输入")
    private String input;

    @Schema(description = "调用输出")
    private String output;

    @Schema(description = "输入 Token 数")
    private long inputTokens;

    @Schema(description = "输出 Token 数")
    private long outputTokens;

    @Schema(description = "是否取得真实用量")
    private boolean usageKnown;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    private Long cachedTokens;
}
