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


    @Operation(summary = "获取访问趋势", description = "获取最近7天的访问趋势")
    @GetMapping("/visit-trend")
    @RequirePermission(dynamic = true)
    public R<List<VisitTrendVO>> getVisitTrend() {
        List<VisitTrendVO> result = dashboardService.getVisitTrend();
        return R.ok(result);
    }


    @Operation(summary = "获取用户分布", description = "获取用户地域分布")
    @GetMapping("/user-distribution")
    @RequirePermission(dynamic = true)
    public R<List<UserDistributionVO>> getUserDistribution() {
        List<UserDistributionVO> result = dashboardService.getUserDistribution();
        return R.ok(result);
    }
}