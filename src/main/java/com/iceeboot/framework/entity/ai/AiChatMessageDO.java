/**
 * AI对话消息实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI对话消息实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_chat_message")
@Schema(description = "大模型对话信息")
public class AiChatMessageDO extends IceeBootDO {

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