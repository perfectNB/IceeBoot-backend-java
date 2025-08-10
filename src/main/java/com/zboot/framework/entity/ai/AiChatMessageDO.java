/**
 * AI对话消息实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zboot.common.core.domain.ZbootDO;
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
@TableName("ai_chat_message")
public class AiChatMessageDO extends ZbootDO {

    /**
     * 会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 角色（user-用户，assistant-助手，system-系统）
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 时间戳
     */
    @TableField("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
}