/**
 * 大模型密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
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
@TableName("ai_llm_key")
public class AiLLMKeyDO extends ZbootDO {

    /**
     * 大模型ID
     */
    @TableField("llm_id")
    private Long llmId;

    /**
     * 密钥
     */
    @TableField("key")
    private String key;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 秘密密钥
     */
    @TableField("secret_key")
    private String secretKey;

    /**
     * API密钥
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 状态（active-激活，inactive-禁用）
     */
    @TableField("status")
    private String status;
}