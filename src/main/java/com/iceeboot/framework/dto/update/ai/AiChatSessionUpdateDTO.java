package com.iceeboot.framework.dto.update.ai;

import com.iceeboot.framework.dto.create.ai.AiChatMessageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI对话会话更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "AI对话会话更新数据传输对象")
public class AiChatSessionUpdateDTO {

    @Schema(description = "主键ID")
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