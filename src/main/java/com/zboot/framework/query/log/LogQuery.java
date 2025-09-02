/**
 * 日志查询DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.query.log;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日志查询D
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "log查询对象")
public class LogQuery extends BaseQuery {



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