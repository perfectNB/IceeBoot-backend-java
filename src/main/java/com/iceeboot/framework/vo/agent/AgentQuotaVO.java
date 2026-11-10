package com.iceeboot.framework.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "当前用户每日模型请求额度，按 UTC 日期计数")
public class AgentQuotaVO {
    private LocalDate usageDay;
    private long usedRequests;

    @Schema(description = "0 表示不设每日上限")
    private int dailyLimit;

    @Schema(description = "不设上限时为 null")
    private Long remainingRequests;
}
