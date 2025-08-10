/**
 * AI对话会话实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * AI对话会话实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_session")
public class AiChatSessionDO extends ZbootDO {

    /**
     * 会话标题
     */
    @TableField("title")
    private String title;

    /**
     * 模型ID
     */
    @TableField("model_id")
    private Long modelId;

    /**
     * 模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 消息列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<AiChatMessageDO> messages;
}