/**
 * AI对话消息实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI对话消息实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_chat_message")
public class AiChatMessageDO extends ZbootDO {

    @Schema(description = "会话ID")
    @TableField("session_id")
    private Long sessionId;

    @Schema(description = "角色（user-用户，assistant-助手，system-系统）")
    @TableField("role")
    private String role;

    @Schema(description = "消息内容")
    @TableField("content")
    private String content;

}