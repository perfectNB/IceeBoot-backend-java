package com.zboot.framework.query.ai;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI对话会话查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI对话会话查询对象")
public class AiChatSessionQuery extends BaseQuery {

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}