package com.zboot.framework.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * AI大模型数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI大模型数据传输对象")
public class AiLLMDTO {

    @Schema(description = "模型ID")
    private Long id;

    @Schema(description = "模型名称")
    @NotBlank(message = "模型名称不能为空")
    private String name;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "密钥列表")
    private List<AiLLMKeyDTO> keys;
}