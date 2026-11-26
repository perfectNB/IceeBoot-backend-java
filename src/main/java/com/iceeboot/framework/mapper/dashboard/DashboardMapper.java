package com.iceeboot.framework.mapper.dashboard;

import com.iceeboot.framework.vo.dashboard.DashboardStatsVO;
import com.iceeboot.framework.vo.dashboard.UserDistributionVO;
import com.iceeboot.framework.vo.dashboard.VisitTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("""
            SELECT accounts.totalUsers, accounts.newUsers, logs.logCount, logs.errorCount
            FROM (SELECT COUNT(*) AS totalUsers,
                         COUNT(CASE WHEN create_time >= #{start} AND create_time < #{end} THEN 1 END) AS newUsers
                  FROM t_user WHERE del_flag = 0) accounts
            CROSS JOIN (SELECT COUNT(*) AS logCount, COUNT(CASE WHEN level = 'ERROR' THEN 1 END) AS errorCount
                        FROM t_log WHERE del_flag = 0 AND timestamp >= #{start} AND timestamp < #{end}) logs
            """)
    DashboardStatsVO stats(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
            SELECT DATE(timestamp) AS date, COUNT(*) AS logCount, COUNT(CASE WHEN level = 'ERROR' THEN 1 END) AS errorCount
            FROM t_log WHERE del_flag = 0 AND timestamp >= #{start} AND timestamp < #{end}
            GROUP BY DATE(timestamp) ORDER BY DATE(timestamp)
            """)
    List<VisitTrendVO> trend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
            SELECT CASE WHEN status IN ('active','inactive') THEN LOWER(status) ELSE 'other' END AS name, COUNT(*) AS value
            FROM t_user WHERE del_flag = 0
            GROUP BY CASE WHEN status IN ('active','inactive') THEN LOWER(status) ELSE 'other' END
            ORDER BY name
            """)
    List<UserDistributionVO> distribution();
}
