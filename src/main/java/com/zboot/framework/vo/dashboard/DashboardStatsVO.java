/**
 * 工作台统计VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作台统计VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class DashboardStatsVO {

    @Schema(description = "总用户数")
    private Integer totalUsers;

    @Schema(description = "今日访问量")
    private Integer todayVisits;

    @Schema(description = "总文章数")
    private Integer totalArticles;

    @Schema(description = "系统状态")
    private String systemStatus;

    @Schema(description = "用户增长率")
    private Double userGrowth;

    @Schema(description = "访问增长率")
    private Double visitGrowth;

    @Schema(description = "文章增长率")
    private Double articleGrowth;
}