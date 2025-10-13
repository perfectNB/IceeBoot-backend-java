package com.iceeboot.framework.query.ai;

import com.iceeboot.common.core.domain.BaseQuery;
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

    @Schema(description = "大模型厂商")
    private String llmProvider;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型类型 1AI大模型 2Rag")
    private Integer type;

}