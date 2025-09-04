/**
 * 大模型密钥实体类
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
 * 大模型密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_llm_key")
public class AiLLMKeyDO extends ZbootDO {

    @Schema(description = "大模型厂商")
    @TableField("llm_provider")
    private String llmProvider;



    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "秘密密钥")
    @TableField("secret_key")
    private String secretKey;

    @Schema(description = "API密钥")
    @TableField("api_key")
    private String apiKey;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;
}