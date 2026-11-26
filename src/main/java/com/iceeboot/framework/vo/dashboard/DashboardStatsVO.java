package com.iceeboot.framework.vo.dashboard;

import lombok.Data;

@Data
public class DashboardStatsVO {
    private long totalUsers;
    private long newUsers;
    private long logCount;
    private long errorCount;
}
