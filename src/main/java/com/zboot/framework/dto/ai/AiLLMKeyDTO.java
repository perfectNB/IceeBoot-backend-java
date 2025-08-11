package com.zboot.framework.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AI大模型密钥数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI大模型密钥数据传输对象")
public class AiLLMKeyDTO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "大模型ID")
    @NotNull(message = "大模型ID不能为空")
    private Long llmId;

    @Schema(description = "密钥")
    private String key;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "秘密密钥")
    private String secretKey;

    @Schema(description = "API密钥")
    private String apiKey;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;
}