package com.iceeboot.framework.dto.create.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI对话会话数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI对话会话数据传输对象")
public class AiChatSessionDTO {

    @Schema(description = "会话标题")
    @NotBlank(message = "会话标题不能为空")
    private String title;

    @Schema(description = "密钥id")
    @NotNull(message = "密钥id不可为空")
    private Long keyId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "会话类型")
    private Integer type;



}