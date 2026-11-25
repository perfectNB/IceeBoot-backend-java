/**
 * 工作台Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.service.dashboard;

import com.iceeboot.framework.vo.dashboard.DashboardStatsVO;
import com.iceeboot.framework.vo.dashboard.UserDistributionVO;
import com.iceeboot.framework.vo.dashboard.VisitTrendVO;

import java.util.List;

/**
 * 工作台Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface DashboardService {

    /**
     * 获取工作台统计数据
     * 
     * @return 统计数据
     */
    DashboardStatsVO getDashboardStats();

    /**
     * 获取访问趋势
     * 
     * @return 访问趋势列表
     */
    List<VisitTrendVO> getVisitTrend(int days);

    /**
     * 获取用户分布
     * 
     * @return 用户分布列表
     */
    List<UserDistributionVO> getUserDistribution();
}