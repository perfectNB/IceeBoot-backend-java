package com.zboot.framework.query.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}