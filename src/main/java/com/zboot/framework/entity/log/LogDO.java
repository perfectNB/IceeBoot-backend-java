/**
 * 日志实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.log;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 日志实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_log")
public class LogDO extends ZbootDO {

    @Schema(description = "日志级别")
    @TableField("level")
    private String level;

    @Schema(description = "模块")
    @TableField("module")
    private String module;

    @Schema(description = "消息")
    @TableField("message")
    private String message;

    @Schema(description = "线程")
    @TableField("thread")
    private String thread;

    @Schema(description = "类名")
    @TableField("class_name")
    private String className;

    @Schema(description = "堆栈跟踪")
    @TableField("stack_trace")
    private String stackTrace;

    @Schema(description = "接口")
    @TableField("summary")
    private String summary;

    @Schema(description = "地址")
    @TableField("path")
    private String path;

    @Schema(description = "时间戳")
    @TableField("timestamp")
    private LocalDateTime timestamp;
}