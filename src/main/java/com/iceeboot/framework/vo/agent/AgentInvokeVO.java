package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** AgentInvokeVO响应视图，敏感字段由服务脱敏。 */
@Data
@Schema(description = "AgentInvokeVO")
public class AgentInvokeVO {
    @Schema(description = "执行记录 ID")
    private String runId;

    private String sessionId;

    @Schema(description = "调用输出")
    private String output;

    @Schema(description = "输入 Token 数")
    private long inputTokens;

    @Schema(description = "输出 Token 数")
    private long outputTokens;

    @Schema(description = "是否取得真实用量")
    private boolean usageKnown;
}
