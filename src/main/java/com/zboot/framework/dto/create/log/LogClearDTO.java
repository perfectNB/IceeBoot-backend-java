/**
 * 日志清空DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.create.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 日志清空DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class LogClearDTO {

    @Schema(description = "日志级别")
    private String level;

    @Schema(description = "清空指定日期之前的日志")
    private String beforeDate;
}