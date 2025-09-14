package com.iceeboot.framework.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI对话消息视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI对话消息视图对象")
public class AiChatMessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "角色（user-用户，assistant-助手，system-系统）")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "时间戳")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}