/**
 * 工作台统计实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.dashboard;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 工作台统计实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_dashboard_stats")
public class DashboardDO extends ZbootDO {

    @Schema(description = "统计日期")
    @TableField("stat_date")
    private LocalDate statDate;

    @Schema(description = "总用户数")
    @TableField("total_users")
    private Integer totalUsers;

    @Schema(description = "今日访问量")
    @TableField("today_visits")
    private Integer todayVisits;

    @Schema(description = "总文章数")
    @TableField("total_articles")
    private Integer totalArticles;

    @Schema(description = "系统状态")
    @TableField("system_status")
    private String systemStatus;

    @Schema(description = "用户增长率")
    @TableField("user_growth")
    private Double userGrowth;

    @Schema(description = "访问增长率")
    @TableField("visit_growth")
    private Double visitGrowth;

    @Schema(description = "文章增长率")
    @TableField("article_growth")
    private Double articleGrowth;
}