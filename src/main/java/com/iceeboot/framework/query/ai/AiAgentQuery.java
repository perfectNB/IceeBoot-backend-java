package com.iceeboot.framework.query.ai;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI代理查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI代理查询对象")
public class AiAgentQuery extends BaseQuery {

    @Schema(description = "代理名称")
    private String name;

    @Schema(description = "代理类型（workflow-工作流，agent-代理）")
    private String type;

    @Schema(description = "平台（coze-扣子，dify-Dify）")
    private String platform;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}