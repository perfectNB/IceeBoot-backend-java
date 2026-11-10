package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_usage_quota")
@Schema(description = "用户每日模型请求账本")
public class AgentUsageQuotaDO extends IceeBootDO {
    @TableField("user_id")
    private String userId;

    @TableField("usage_day")
    private LocalDate usageDay;

    @TableField("used_requests")
    private Long usedRequests;
}
