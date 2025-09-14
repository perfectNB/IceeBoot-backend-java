/**
 * 消息创建数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.iceeboot.framework.dto.create.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 消息创建数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@Schema(description = "消息创建数据传输对象")
public class MessageDTO {

    @Schema(description = "收信息人ID")
    @NotNull(message = "收信息人ID不能为空")
    private Long userId;

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "内容")
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "类型（system-系统消息，notice-通知消息，warning-警告消息）")
    @NotBlank(message = "类型不能为空")
    private String type;

    @Schema(description = "状态（unread-未读，read-已读）")
    private String status;
}