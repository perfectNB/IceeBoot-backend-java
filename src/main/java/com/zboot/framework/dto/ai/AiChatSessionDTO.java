package com.zboot.framework.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * AI对话会话数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI对话会话数据传输对象")
public class AiChatSessionDTO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标题")
    @NotBlank(message = "会话标题不能为空")
    private String title;

    @Schema(description = "模型ID")
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "消息列表")
    private List<AiChatMessageDTO> messages;
}