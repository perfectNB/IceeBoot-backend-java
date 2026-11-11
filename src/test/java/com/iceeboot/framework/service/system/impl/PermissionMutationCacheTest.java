package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.framework.dto.create.system.PermissionDTO;
import com.iceeboot.framework.dto.update.system.PermissionUpdateDTO;
import com.iceeboot.framework.entity.system.PermissionDO;
import com.iceeboot.framework.mapper.system.SysPermissionMapper;
import com.iceeboot.framework.service.database.DatabaseInitService;
import com.iceeboot.framework.service.system.ISysRolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionMutationCacheTest {
    private SysPermissionServiceImpl service;
    private AuthCacheServiceImpl auth;
    private TransactionTemplate transactions;
    private RedisTemplate<String, Object> redis;
    private JdbcTemplate jdbc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:permission_mutation_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        String ddl = ReflectionTestUtils.invokeMethod(new DatabaseInitService(null),
                "generateCreateTableSql", PermissionDO.class, "t_permission");
        jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        jdbc.update("INSERT INTO t_permission(id,name,code,type,parent_id,path,method,status,del_flag)"
                + " VALUES(10,'读取','sample:read','api',0,'/sample','GET','active',0)");
        var config = new MybatisConfiguration();
        config.addMapper(SysPermissionMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setConfiguration(config);
        var mapper = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject())).getMapper(SysPermissionMapper.class);
        redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var cached = new AtomicReference<Object>();
        when(values.get(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY)).thenAnswer(call -> cached.get());
        doAnswer(call -> { cached.set(call.getArgument(1)); return null; })
                .when(values).set(eq(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY), any());
        when(redis.delete(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY)).thenAnswer(call -> {
            cached.set(null); return true;
        });
        var rolePermissions = mock(ISysRolePermissionService.class);
        when(rolePermissions.getRoleIdsByPermissionId(anyLong())).thenReturn(List.of());
        service = new SysPermissionServiceImpl(rolePermissions, redis);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        auth = new AuthCacheServiceImpl(redis, null, mapper, null, null, null, null);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(ds));
        assertThat(auth.getUrlPermissions("/sample", "GET")).containsExactly("sample:read");
        clearInvocations(redis);
    }

    @Test
    void changedRouteAndMethodInvalidateOnlyAfterCommit() {
        var dto = new PermissionUpdateDTO();
        dto.setId(10L);dto.setCode("sample:write");dto.setName("写入");
        dto.setPath("/changed");dto.setMethod("POST");
        transactions.executeWithoutResult(status -> {
            assertThat(service.updatePermission(dto)).isTrue();
            verify(redis, never()).delete(anyString());
        });
        assertThat(auth.getUrlPermissions("/sample", "GET")).isEmpty();
        assertThat(auth.getUrlPermissions("/changed", "POST")).containsExactly("sample:write");
        assertThat(auth.getUrlPermissions("/changed", "GET")).isEmpty();
    }

    @Test
    void addingDisablingReenablingAndDeletingPermissionsRefreshesTheMapping() {
        var dto = new PermissionDTO();
        dto.setName("新增接口");dto.setCode("sample:new");dto.setType("api");
        dto.setPath("/new");dto.setMethod("POST");dto.setStatus("active");dto.setParentId(0L);
        transactions.executeWithoutResult(status -> assertThat(service.addPermission(dto)).isTrue());
        assertThat(auth.getUrlPermissions("/new", "POST")).containsExactly("sample:new");
        transactions.executeWithoutResult(status -> assertThat(service.updateStatus(10L, "inactive")).isTrue());
        assertThat(auth.getUrlPermissions("/sample", "GET")).isEmpty();
        transactions.executeWithoutResult(status -> assertThat(service.updateStatus(10L, "active")).isTrue());
        assertThat(auth.getUrlPermissions("/sample", "GET")).containsExactly("sample:read");
        transactions.executeWithoutResult(status -> assertThat(service.deletePermissions(List.of(10L))).isTrue());
        assertThat(auth.getUrlPermissions("/sample", "GET")).isEmpty();
    }

    @Test
    void rollbackAndMissingUpdatesDoNotInvalidateUnchangedConfiguration() {
        transactions.executeWithoutResult(status -> {
            service.updateStatus(10L, "inactive");
            status.setRollbackOnly();
        });
        verify(redis, never()).delete(anyString());
        assertThat(jdbc.queryForObject("SELECT status FROM t_permission WHERE id=10", String.class)).isEqualTo("active");
        assertThat(auth.getUrlPermissions("/sample", "GET")).containsExactly("sample:read");
        transactions.executeWithoutResult(status -> assertThat(service.updateStatus(999L, "active")).isFalse());
        verify(redis, never()).delete(anyString());
    }
}
