/**
 * 大模型密钥实体类
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
 * 大模型密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_llm_key")
@Schema(description = "大模型密钥")
public class AiLLMKeyDO extends IceeBootDO {

    @Schema(description = "密钥名称（同一厂商下唯一，用于按名路由）")
    @TableField("name")
    private String name;

    @Schema(description = "大模型厂商")
    @TableField("llm_provider")
    private String llmProvider;

    @Schema(description = "自定义请求地址")
    @TableField("url")
    private String url;

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
