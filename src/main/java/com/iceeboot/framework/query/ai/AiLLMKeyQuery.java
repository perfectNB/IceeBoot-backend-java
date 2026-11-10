package com.iceeboot.framework.query.ai;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI大模型密钥查询对象
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI大模型密钥查询对象")
public class AiLLMKeyQuery extends BaseQuery {

    @Schema(description = "密钥名称")
    private String name;

    @Schema(description = "大模型厂商")
    private String llmProvider;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
