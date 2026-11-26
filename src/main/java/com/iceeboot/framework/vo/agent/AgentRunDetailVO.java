package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;

/** AgentRunDetailVO响应视图，敏感字段由服务脱敏。 */
@Data
@Schema(description = "AgentRunDetailVO")
public class AgentRunDetailVO {
    @Schema(description = "父子执行记录")
    private List<AgentRunVO> agents;

    @Schema(description = "模型调用明细")
    private List<AgentModelCallVO> modelCalls;

    @Schema(description = "总输入 Token 数")
    private long totalInputTokens;

    @Schema(description = "总输出 Token 数")
    private long totalOutputTokens;

    private Long totalCachedTokens;

    @Schema(description = "是否取得真实用量")
    private boolean usageKnown;
}
