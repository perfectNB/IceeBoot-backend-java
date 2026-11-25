package com.iceeboot.framework.service.dashboard.impl;

import com.iceeboot.framework.mapper.dashboard.DashboardMapper;
import com.iceeboot.framework.service.dashboard.DashboardService;
import com.iceeboot.framework.vo.dashboard.DashboardStatsVO;
import com.iceeboot.framework.vo.dashboard.UserDistributionVO;
import com.iceeboot.framework.vo.dashboard.VisitTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardMapper mapper;

    @Override
    public DashboardStatsVO getDashboardStats() {
        LocalDate today = LocalDate.now();
        return mapper.stats(today, today.plusDays(1));
    }

    @Override
    public List<VisitTrendVO> getVisitTrend(int days) {
        LocalDate end = LocalDate.now().plusDays(1);
        LocalDate start = end.minusDays(days);
        Map<String, VisitTrendVO> recorded = new HashMap<>();
        for (VisitTrendVO row : mapper.trend(start, end)) {
            recorded.put(row.getDate(), row);
        }
        List<VisitTrendVO> result = new ArrayList<>(days);
        for (LocalDate day = start; day.isBefore(end); day = day.plusDays(1)) {
            VisitTrendVO row = recorded.get(day.toString());
            if (row == null) {
                row = new VisitTrendVO();
                row.setDate(day.toString());
            }
            result.add(row);
        }
        return result;
    }

    @Override
    public List<UserDistributionVO> getUserDistribution() {
        return mapper.distribution();
    }
}
