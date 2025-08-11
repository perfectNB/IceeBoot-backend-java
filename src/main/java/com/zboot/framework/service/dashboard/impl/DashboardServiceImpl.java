package com.zboot.framework.service.dashboard.impl;


import com.zboot.framework.service.dashboard.DashboardService;
import com.zboot.framework.vo.dashboard.DashboardStatsVO;
import com.zboot.framework.vo.dashboard.UserDistributionVO;
import com.zboot.framework.vo.dashboard.VisitTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dashboard服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    @Override
    public DashboardStatsVO getDashboardStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        
        // 模拟数据，实际项目中应该从数据库查询
        stats.setTotalUsers(1250);
        stats.setTodayVisits(320);
        stats.setTotalArticles(89);
        stats.setSystemStatus("正常");
        stats.setUserGrowth(12.5);
        stats.setVisitGrowth(8.3);
        stats.setArticleGrowth(15.2);
        
        return stats;
    }

    @Override
    public List<VisitTrendVO> getVisitTrend() {
        List<VisitTrendVO> trendList = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 6; i >= 0; i--) {
            VisitTrendVO trend = new VisitTrendVO();
            trend.setDate(LocalDate.now().minusDays(i).toString());
            trend.setVisits(200 + random.nextInt(300)); // 200-500之间的随机访问量
            trend.setUsers(50 + random.nextInt(100));   // 50-150之间的随机用户数
            trendList.add(trend);
        }
        
        return trendList;
    }

    @Override
    public List<UserDistributionVO> getUserDistribution() {
        List<UserDistributionVO> distributionList = new ArrayList<>();
        
        // 模拟用户分布数据
        UserDistributionVO beijing = new UserDistributionVO();
        beijing.setName("北京");
        beijing.setValue(320);
        beijing.setPercentage(25.6);
        distributionList.add(beijing);
        
        UserDistributionVO shanghai = new UserDistributionVO();
        shanghai.setName("上海");
        shanghai.setValue(280);
        shanghai.setPercentage(22.4);
        distributionList.add(shanghai);
        
        UserDistributionVO guangzhou = new UserDistributionVO();
        guangzhou.setName("广州");
        guangzhou.setValue(180);
        guangzhou.setPercentage(14.4);
        distributionList.add(guangzhou);
        
        UserDistributionVO shenzhen = new UserDistributionVO();
        shenzhen.setName("深圳");
        shenzhen.setValue(150);
        shenzhen.setPercentage(12.0);
        distributionList.add(shenzhen);
        
        UserDistributionVO hangzhou = new UserDistributionVO();
        hangzhou.setName("杭州");
        hangzhou.setValue(120);
        hangzhou.setPercentage(9.6);
        distributionList.add(hangzhou);
        
        UserDistributionVO others = new UserDistributionVO();
        others.setName("其他");
        others.setValue(200);
        others.setPercentage(16.0);
        distributionList.add(others);
        
        return distributionList;
    }
}