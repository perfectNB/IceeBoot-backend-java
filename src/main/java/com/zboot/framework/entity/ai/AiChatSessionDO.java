/**
 * AI对话会话实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI对话会话实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_chat_session")
public class AiChatSessionDO extends ZbootDO {

    @Schema(description = "会话标题")
    @TableField("title")
    private String title;

    @Schema(description = "模型ID")
    @TableField("model_id")
    private Long modelId;

    @Schema(description = "模型名称")
    @TableField("model_name")
    private String modelName;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;
}