package com.zboot.framework.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI大模型厂商视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Schema(description = "AI大模型厂商视图对象")
public class AiLLMProviderVO {

    @Schema(description = "厂商名称")
    private String providerName;

    @Schema(description = "厂商显示名称")
    private String displayName;

    @Schema(description = "密钥列表")
    private List<AiLLMKeyVO> keys;

    @Schema(description = "密钥数量")
    private Integer keyCount;

    @Schema(description = "激活密钥数量")
    private Integer activeKeyCount;
}