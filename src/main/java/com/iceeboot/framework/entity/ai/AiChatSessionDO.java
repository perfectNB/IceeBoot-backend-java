/**
 * AI对话会话实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.entity.ai;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
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
@Schema(description = "大模型对话")
public class AiChatSessionDO extends IceeBootDO {

    @Schema(description = "会话标题")
    @TableField("title")
    private String title;

    @Schema(description = "模型名称")
    @TableField("model_name")
    private String modelName;

    @Schema(description = "密钥ID")
    @TableField("key_id")
    private Long keyId;

    @Schema(description = "对话类型 1 AI大模型 2 Rag知识库")
    @TableField("type")
    private Integer type;
}