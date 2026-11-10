package com.iceeboot.framework.dto.create.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI大模型密钥数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Schema(description = "AI大模型密钥数据传输对象")
public class AiLLMKeyDTO {

    @Schema(description = "密钥名称（同一厂商下唯一，例如 primary-deepseek / backup-deepseek）")
    private String name;

    @Schema(description = "大模型厂商")
    @NotBlank(message = "大模型厂商不能为空")
    private String llmProvider;

    @Schema(description = "自定义请求地址")
    private String url;

    @Schema(description = "秘密密钥")
    private String secretKey;

    @Schema(description = "API密钥")
    private String apiKey;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
