package com.zboot.framework.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * AI对话消息数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI对话消息数据传输对象")
public class AiChatMessageDTO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @Schema(description = "角色（user-用户，assistant-助手，system-系统）")
    @NotBlank(message = "角色不能为空")
    private String role;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "时间戳")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "用户ID")
    private Long userId;
}