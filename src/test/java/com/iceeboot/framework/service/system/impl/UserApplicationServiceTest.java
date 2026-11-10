package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.framework.entity.system.UserApplicationDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.mapper.system.SysUserMapper;
import com.iceeboot.framework.mapper.system.UserApplicationMapper;
import com.iceeboot.framework.service.database.DatabaseInitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;

class UserApplicationServiceTest {
    UserApplicationServiceImpl service;
    UserApplicationMapper mapper;
    TransactionTemplate tx;
    @BeforeEach void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:apps_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        for (var entity : List.of(UserDO.class, UserApplicationDO.class)) {
            String table = entity == UserDO.class ? "t_user" : "t_user_application";
            String ddl = ReflectionTestUtils.invokeMethod(new DatabaseInitService(null),
                    "generateCreateTableSql", entity, table);
            jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        }
        var cfg = new MybatisConfiguration();
        cfg.addMapper(SysUserMapper.class); cfg.addMapper(UserApplicationMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds); factory.setConfiguration(cfg);
        var sql = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        var users = sql.getMapper(SysUserMapper.class);
        var user = new UserDO(); user.setId(7L); users.insert(user);
        mapper = sql.getMapper(UserApplicationMapper.class);
        service = new UserApplicationServiceImpl(mapper, users);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }
    @Test void repeatedEntryKeepsOneRelationshipAndFirstLoginTime() {
        Boolean first = tx.execute(s -> service.enterApplication(7L, " portal "));
        var original = mapper.selectList(null).getFirst();
        Boolean second = tx.execute(s -> service.enterApplication(7L, "PORTAL"));
        assertThat(first).isTrue(); assertThat(second).isFalse();
        assertThat(mapper.selectCount(null)).isEqualTo(1);
        assertThat(mapper.selectList(null).getFirst().getFirstLoginTime()).isEqualTo(original.getFirstLoginTime());
    }
    @Test void concurrentEntryCreatesOneRelationshipWithoutDependingOnUniqueIndex() throws Exception {
        var start = new CyclicBarrier(2);
        Callable<Boolean> enter = () -> {
            start.await(); return tx.execute(s -> service.enterApplication(7L, "PLATFORM"));
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(enter); var second = executor.submit(enter);
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(mapper.selectCount(null)).isEqualTo(1);
    }
}
