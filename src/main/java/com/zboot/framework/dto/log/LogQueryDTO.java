/**
 * 日志查询DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 日志查询DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class LogQueryDTO {

    @Schema(description = "页码")
    private Integer page = 1;

    @Schema(description = "每页数量")
    private Integer size = 10;

    @Schema(description = "日志级别筛选")
    private String level;

    @Schema(description = "模块筛选")
    private String module;

    @Schema(description = "关键词搜索")
    private String keyword;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}