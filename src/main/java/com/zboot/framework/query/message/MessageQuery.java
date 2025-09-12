/**
 * 消息查询对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.zboot.framework.query.message;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息查询对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息查询对象")
public class MessageQuery extends BaseQuery {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态（unread-未读，read-已读）")
    private String status;

    @Schema(description = "类型（system-系统消息，notice-通知消息，warning-警告消息）")
    private String type;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}