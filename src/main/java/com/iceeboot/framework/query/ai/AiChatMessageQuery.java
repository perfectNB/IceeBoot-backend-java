/**
 * AI聊天消息查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.query.ai;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI聊天消息查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI聊天消息查询对象")
public class AiChatMessageQuery extends BaseQuery {

    @Schema(description = "会话ID")
    private Long sessionId;
    

}