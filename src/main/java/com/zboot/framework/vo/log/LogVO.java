/**
 * 日志VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.vo.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class LogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "日志级别")
    private String level;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "线程")
    private String thread;

    @Schema(description = "类名")
    private String className;

    @Schema(description = "堆栈跟踪")
    private String stackTrace;

    @Schema(description = "时间戳")
    private LocalDateTime timestamp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}