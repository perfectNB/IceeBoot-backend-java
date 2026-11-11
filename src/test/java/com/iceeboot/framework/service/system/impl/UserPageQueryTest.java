package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.framework.entity.system.*;
import com.iceeboot.framework.mapper.system.SysUserMapper;
import com.iceeboot.framework.query.system.SysUserQuery;
import com.iceeboot.framework.service.database.DatabaseInitService;
import com.iceeboot.framework.service.system.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.*;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserPageQueryTest {
    @Intercepts({
            @Signature(type = Executor.class, method = "query",
                    args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
            @Signature(type = Executor.class, method = "query",
                    args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                            org.apache.ibatis.cache.CacheKey.class, org.apache.ibatis.mapping.BoundSql.class})
    })
    static class QueryCounter implements Interceptor {
        final AtomicInteger calls = new AtomicInteger();
        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            calls.incrementAndGet();
            return invocation.proceed();
        }
    }

    @Test
    void largerPagesUseConstantQueriesAndPreserveCurrentRoleAssignments() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:user_page_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        for (var type : List.of(UserDO.class, RoleDO.class, UserRoleDO.class)) {
            String table = type.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class).value();
            String ddl = ReflectionTestUtils.invokeMethod(new DatabaseInitService(null),
                    "generateCreateTableSql", type, table);
            jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        }
        jdbc.update("INSERT INTO t_role(id,name,code,status,sort,del_flag) VALUES"
                + " (42,'有效角色','reader','active',1,0),(43,'禁用角色','disabled','inactive',2,0),"
                + " (44,'删除角色','deleted','active',3,1),(45,'撤销关联','revoked','active',4,0)");
        for (int id = 1; id <= 24; id++) {
            jdbc.update("INSERT INTO t_user(id,username,password,create_time,del_flag) VALUES(?,?,?,CURRENT_TIMESTAMP,0)",
                    id, "user" + id, "must-not-leak");
            jdbc.update("INSERT INTO t_user_role(user_id,role_id,del_flag) VALUES(?,42,0)", id);
        }
        jdbc.update("INSERT INTO t_user_role(user_id,role_id,del_flag) VALUES(1,42,0),(1,43,0),(1,44,0),(1,45,1)");
        var pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        var configuration = new MybatisConfiguration();
        var counter = new QueryCounter();
        configuration.addInterceptor(counter);
        configuration.addInterceptor(pagination);
        configuration.addMapper(SysUserMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/system/SysUserMapper.xml"));
        var mapper = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject())).getMapper(SysUserMapper.class);
        var auth = mock(IAuthCacheService.class);
        var roles = mock(ISysRoleService.class);
        var service = new SysUserServiceImpl(mapper, roles, auth, mock(RedisTemplate.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        var query = new SysUserQuery();
        query.setPageNum(1);
        query.setPageSize(5);
        var first = service.getUserPage(query);
        int smallerPageQueries = counter.calls.get();
        counter.calls.set(0);
        query.setPageSize(20);
        var larger = service.getUserPage(query);
        assertThat(counter.calls.get()).isEqualTo(smallerPageQueries).isBetween(2, 3);
        assertThat(larger.getTotal()).isEqualTo(24);
        assertThat(larger.getRecords()).hasSize(20);
        assertThat(first.getRecords()).hasSize(5);
        for (var user : larger.getRecords()) {
            assertThat(user.getRoleList()).extracting(role -> role.getCode())
                    .contains("reader").doesNotContain("deleted", "revoked");
        }
        var batch = mapper.selectRoleSummariesByUserIds(List.of(1L, 2L));
        assertThat(batch).hasSize(3);
        assertThat(batch.stream().filter(role -> role.getUserId().equals(1L)).toList())
                .extracting(role -> role.getCode()).containsExactly("reader", "disabled");
        String serialized = new ObjectMapper().findAndRegisterModules().writeValueAsString(larger);
        assertThat(serialized).doesNotContain("must-not-leak", "password", "permissions", "menus", "userId");
        verifyNoInteractions(auth, roles);
        counter.calls.set(0);
        query.setPageNum(100);
        assertThat(service.getUserPage(query).getRecords()).isEmpty();
        assertThat(counter.calls.get()).isLessThanOrEqualTo(2);
    }
}
