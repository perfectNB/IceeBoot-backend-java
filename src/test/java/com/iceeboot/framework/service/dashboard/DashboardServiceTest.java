package com.iceeboot.framework.service.dashboard;

import com.iceeboot.framework.mapper.dashboard.DashboardMapper;
import com.iceeboot.framework.service.dashboard.impl.DashboardServiceImpl;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {
    @Test
    void countsOnlyLiveRecordsAndFillsDatesWithoutLogs() throws Exception {
        var source = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;NON_KEYWORDS=VALUE", "sa", "");
        try (var connection = source.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE t_user (status VARCHAR(20), create_time TIMESTAMP, del_flag INT)");
            sql.execute("CREATE TABLE t_log (level VARCHAR(20), timestamp TIMESTAMP, del_flag INT)");
            var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), source));
            config.addMapper(DashboardMapper.class);
            try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
                var service = new DashboardServiceImpl(session.getMapper(DashboardMapper.class));
                assertEquals(0, service.getDashboardStats().getTotalUsers());
                var today = LocalDate.now();
                sql.execute("INSERT INTO t_user VALUES ('active','" + today + "',0),('inactive',NULL,0),('active',NULL,1)");
                sql.execute("INSERT INTO t_log VALUES ('ERROR','" + today + "',0),('WARN','" + today
                        + "',0),('ERROR','" + today + "',1),('ERROR','" + today.plusDays(1) + "',0)");
                session.clearCache();
                var stats = service.getDashboardStats();
                assertEquals(2, stats.getTotalUsers());
                assertEquals(1, stats.getNewUsers());
                assertEquals(2, stats.getLogCount());
                assertEquals(1, stats.getErrorCount());
                for (int days : new int[]{7, 30}) {
                    var trend = service.getVisitTrend(days);
                    assertEquals(days, trend.size());
                    assertEquals(today.minusDays(days - 1).toString(), trend.getFirst().getDate());
                    assertEquals(0, trend.getFirst().getLogCount());
                    assertEquals(2, trend.getLast().getLogCount());
                    assertEquals(1, trend.getLast().getErrorCount());
                }
                var distribution = service.getUserDistribution();
                assertEquals(2, distribution.size());
                assertEquals("active", distribution.getFirst().getName());
                assertEquals(1, distribution.getFirst().getValue());
            }
        }
    }
}
