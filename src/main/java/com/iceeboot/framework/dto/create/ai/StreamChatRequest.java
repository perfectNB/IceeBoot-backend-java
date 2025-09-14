package com.iceeboot.framework.dto.create.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 流式对话请求实体类
 * 
 * @author CodeIcee
 * @date 2025-09-08
 */
@Data
@Accessors(chain = true)
@Schema(description = "流式对话请求")
public class StreamChatRequest {

    @Schema(description = "会话ID")
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @Schema(description = "对话内容")
    @NotBlank(message = "对话内容不能为空")
    private String content;
}