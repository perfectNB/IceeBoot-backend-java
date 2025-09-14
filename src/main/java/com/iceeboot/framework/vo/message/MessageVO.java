/**
 * 消息视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.iceeboot.framework.vo.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@Schema(description = "消息视图对象")
public class MessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "收信息人ID")
    private Long userId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "类型（system-系统消息，notice-通知消息，warning-警告消息）")
    private String type;

    @Schema(description = "状态（unread-未读，read-已读）")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}