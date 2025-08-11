/**
 * 工作台Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.dashboard;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.entity.dashboard.DashboardDO;
import com.zboot.framework.vo.dashboard.DashboardStatsVO;
import com.zboot.framework.vo.dashboard.UserDistributionVO;
import com.zboot.framework.vo.dashboard.VisitTrendVO;

import java.util.List;

/**
 * 工作台Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface DashboardService extends IService<DashboardDO> {

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
    List<VisitTrendVO> getVisitTrend();

    /**
     * 获取用户分布
     * 
     * @return 用户分布列表
     */
    List<UserDistributionVO> getUserDistribution();
}