/**
 * 消息实体类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.zboot.framework.entity.message;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息实体类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_message")
@Schema(description = "消息")
public class MessageDO extends ZbootDO {

    @Schema(description = "收信息人ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "标题")
    @TableField("title")
    private String title;

    @Schema(description = "内容")
    @TableField("content")
    private String content;

    @Schema(description = "类型（system-系统消息，notice-通知消息，warning-警告消息）")
    @TableField("type")
    private String type;

    @Schema(description = "状态（unread-未读，read-已读）")
    @TableField("status")
    private String status;
}