package com.iceeboot.framework.controller.dashboard;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.service.dashboard.DashboardService;
import com.iceeboot.framework.vo.dashboard.DashboardStatsVO;
import com.iceeboot.framework.vo.dashboard.UserDistributionVO;
import com.iceeboot.framework.vo.dashboard.VisitTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 工作台控制器
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "工作台管理", description = "工作台管理相关接口")
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    @Operation(summary = "获取工作台统计数据", description = "获取工作台统计数据")
    @GetMapping("/stats")
    @RequirePermission(dynamic = true)
    public R<DashboardStatsVO> getDashboardStats() {
        DashboardStatsVO result = dashboardService.getDashboardStats();
        return R.ok(result);
    }


    @Operation(summary = "获取访问趋势", description = "最近 7 或 30 天的日志量与错误量")
    @GetMapping("/visit-trend")
    @RequirePermission(dynamic = true)
    public R<List<VisitTrendVO>> getVisitTrend(@RequestParam(defaultValue = "week") String period) {
        if (!"week".equals(period) && !"month".equals(period)) {
            return R.badRequest("period 仅支持 week 或 month");
        }
        List<VisitTrendVO> result = dashboardService.getVisitTrend("month".equals(period) ? 30 : 7);
        return R.ok(result);
    }


    @Operation(summary = "获取用户分布", description = "按账户状态统计用户数")
    @GetMapping("/user-distribution")
    @RequirePermission(dynamic = true)
    public R<List<UserDistributionVO>> getUserDistribution() {
        List<UserDistributionVO> result = dashboardService.getUserDistribution();
        return R.ok(result);
    }
}