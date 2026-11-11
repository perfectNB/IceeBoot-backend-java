package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.framework.entity.system.*;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.framework.mapper.system.*;
import com.iceeboot.framework.service.database.DatabaseInitService;
import com.iceeboot.framework.service.system.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用真实SQL和事务代理，验证勾选变更只写入关联差集。 */
class RoleGrantPersistenceTest {
    JdbcTemplate jdbc;
    SysRoleServiceImpl service;
    ISysRolePermissionService permissionLinks;
    ISysRoleMenuService menuLinks;
    RedisTemplate<String, Object> redis;
    ValueOperations<String, Object> values;
    final DmlCounter writes = new DmlCounter();
    TransactionTemplate tx;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:grants_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        jdbc = new JdbcTemplate(ds);
        var init = new DatabaseInitService(null);
        var entities = List.of(RoleDO.class, PermissionDO.class, MenuDO.class,
                RolePermissionDO.class, RoleMenuDO.class);
        var tables = List.of("t_role", "t_permission", "t_menu", "t_role_permission", "t_role_menu");
        for (int i = 0; i < tables.size(); i++) {
            String ddl = ReflectionTestUtils.invokeMethod(init, "generateCreateTableSql", entities.get(i), tables.get(i));
            jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        }
        jdbc.update("INSERT INTO t_role(id,code,status,del_flag) VALUES(7,'fixture','active',0)");
        jdbc.update("INSERT INTO t_permission(id,name,status,del_flag) VALUES(1,'one','active',0),(2,'two','active',0),(3,'three','active',0)");
        jdbc.update("INSERT INTO t_menu(id,name,status,del_flag) VALUES(1,'one','active',0),(2,'two','active',0),(3,'three','active',0)");
        var cfg = new MybatisConfiguration();
        cfg.addMapper(SysRoleMapper.class);
        cfg.addMapper(SysRolePermissionMapper.class);
        cfg.addMapper(SysRoleMenuMapper.class);
        cfg.addInterceptor(writes);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds); factory.setConfiguration(cfg);
        var sql = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        var permissionMapper = sql.getMapper(SysRolePermissionMapper.class);
        var menuMapper = sql.getMapper(SysRoleMenuMapper.class);
        var permissionTarget = new SysRolePermissionServiceImpl(permissionMapper);
        ReflectionTestUtils.setField(permissionTarget, "baseMapper", permissionMapper);
        var menuTarget = new SysRoleMenuServiceImpl(menuMapper);
        ReflectionTestUtils.setField(menuTarget, "baseMapper", menuMapper);
        var manager = new DataSourceTransactionManager(ds);
        permissionLinks = transactional(ISysRolePermissionService.class, permissionTarget, manager);
        menuLinks = transactional(ISysRoleMenuService.class, menuTarget, manager);
        redis = mock(RedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        tx = new TransactionTemplate(manager);
        service = new SysRoleServiceImpl(permissionLinks, mock(ISysPermissionService.class), redis,
                mock(ISysUserRoleService.class), mock(ISysMenuService.class), menuLinks,
                tx);
        ReflectionTestUtils.setField(service, "baseMapper", sql.getMapper(SysRoleMapper.class));
    }

    private <T> T transactional(Class<T> contract, T target, DataSourceTransactionManager manager) {
        var proxy = new ProxyFactory(target);
        proxy.setInterfaces(contract);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return contract.cast(proxy.getProxy());
    }

    String table(boolean permission) { return permission ? "t_role_permission" : "t_role_menu"; }
    String resource(boolean permission) { return permission ? "permission_id" : "menu_id"; }
    void seed(boolean permission) {
        jdbc.update("INSERT INTO " + table(permission) + "(id,role_id," + resource(permission)
                + ",del_flag) VALUES(10,7,1,0),(20,7,2,0)");
        writes.commands.clear();
    }
    void save(boolean permission, List<Long> ids) {
        if (permission) assertThat(service.saveRolePermissions(7L, ids)).isTrue();
        else assertThat(service.saveRoleMenus(7L, ids)).isTrue();
    }
    List<Long> ids(boolean permission) {
        return jdbc.queryForList("SELECT " + resource(permission) + " FROM " + table(permission)
                + " WHERE role_id=7 AND del_flag=0 ORDER BY " + resource(permission), Long.class);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void unchangedOrDuplicateIdsPerformNoWritesAndKeepOriginalRows(boolean permission) {
        seed(permission);
        save(permission, List.of(2L,1L,2L,1L));
        assertThat(writes.commands).isEmpty();
        assertThat(jdbc.queryForList("SELECT id FROM " + table(permission)
                + " WHERE del_flag=0 ORDER BY id", Long.class)).containsExactly(10L,20L);
        verifyNoInteractions(redis, values);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void changedSelectionOnlyDeletesAndInsertsDifference(boolean permission) {
        seed(permission);
        save(permission, List.of(2L,3L,3L));
        assertThat(ids(permission)).containsExactly(2L,3L);
        assertThat(jdbc.queryForObject("SELECT id FROM " + table(permission) + " WHERE "
                + resource(permission) + "=2 AND del_flag=0", Long.class)).isEqualTo(20L);
        assertThat(writes.commands).containsExactly(SqlCommandType.DELETE,SqlCommandType.INSERT);
        verify(redis).delete(permission ? CacheConstants.Auth.ROLE_PERMISSIONS_KEY+7 : CacheConstants.Auth.ROLE_MENUS_KEY+7);
        verify(values, never()).set(anyString(),any());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void emptySelectionClearsOnceAndRepeatedClearDoesNothing(boolean permission) {
        seed(permission);
        save(permission, null);
        assertThat(ids(permission)).isEmpty();
        assertThat(writes.commands).containsExactly(SqlCommandType.DELETE);
        clearInvocations(redis,values); writes.commands.clear();
        save(permission,List.of());
        assertThat(writes.commands).isEmpty();
        verifyNoInteractions(redis,values);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void invalidAddedResourceDoesNotRemoveExistingGrants(boolean permission) {
        seed(permission);
        assertThatThrownBy(() -> save(permission,List.of(2L,999L))).isInstanceOf(RuntimeException.class);
        assertThat(ids(permission)).containsExactly(1L,2L);
        assertThat(writes.commands).isEmpty();
        verifyNoInteractions(redis,values);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void insertFailureRollsBackRemovalAndDoesNotPublishCache(boolean permission) {
        seed(permission);
        jdbc.execute("ALTER TABLE " + table(permission) + " ADD CONSTRAINT fail_three CHECK("
                + resource(permission) + "<>3)");
        assertThatThrownBy(() -> save(permission,List.of(2L,3L))).isInstanceOf(RuntimeException.class);
        assertThat(ids(permission)).containsExactly(1L,2L);
        verifyNoInteractions(redis,values);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void cacheFailureAfterCommitDoesNotTurnSuccessfulSaveIntoAnError(boolean permission) {
        seed(permission);
        when(redis.delete(anyString())).thenThrow(
                new org.springframework.data.redis.RedisConnectionFailureException("fixture outage"));
        save(permission,List.of(2L,3L));
        assertThat(ids(permission)).containsExactly(2L,3L);
        verify(redis).delete(permission ? CacheConstants.Auth.ROLE_PERMISSIONS_KEY+7
                : CacheConstants.Auth.ROLE_MENUS_KEY+7);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void outerTransactionDefersInvalidationAndRollbackLeavesOriginalState(boolean permission) {
        seed(permission);
        tx.executeWithoutResult(status -> {
            save(permission,List.of(3L));
            verifyNoInteractions(redis,values);
            status.setRollbackOnly();
        });
        assertThat(ids(permission)).containsExactly(1L,2L);
        verifyNoInteractions(redis,values);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void legacyAssignEntryUsesRealTransactionEvenWhenCallingReplacementInternally(boolean permission) {
        seed(permission);
        jdbc.execute("ALTER TABLE " + table(permission) + " ADD CONSTRAINT fail_three CHECK("
                + resource(permission) + "<>3)");
        assertThatThrownBy(() -> {
            if (permission) permissionLinks.assignPermissionsToRole(7L,List.of(2L,3L));
            else menuLinks.assignMenusToRole(7L,List.of(2L,3L));
        }).isInstanceOf(RuntimeException.class);
        assertThat(ids(permission)).containsExactly(1L,2L);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void historicDeletedLinkDoesNotCountAsAnActiveGrant(boolean permission) {
        jdbc.update("INSERT INTO " + table(permission) + "(id,role_id," + resource(permission)
                + ",del_flag) VALUES(10,7,1,1)");
        writes.commands.clear();
        save(permission,List.of(1L));
        assertThat(ids(permission)).containsExactly(1L);
        assertThat(writes.commands).containsExactly(SqlCommandType.INSERT);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void concurrentReplacementNeverCombinesTwoSubmissions(boolean permission) throws Exception {
        seed(permission);
        var start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a=executor.submit(() -> { start.await(); save(permission,List.of(1L,3L)); return true; });
            var b=executor.submit(() -> { start.await(); save(permission,List.of(2L)); return true; });
            assertThat(a.get()).isTrue(); assertThat(b.get()).isTrue();
        }
        assertThat(ids(permission)).isIn(List.of(1L,3L),List.of(2L));
    }

    @Intercepts(@Signature(type=Executor.class,method="update",args={MappedStatement.class,Object.class}))
    static class DmlCounter implements Interceptor {
        final List<SqlCommandType> commands=Collections.synchronizedList(new ArrayList<>());
        @Override public Object intercept(Invocation invocation) throws Throwable {
            commands.add(((MappedStatement)invocation.getArgs()[0]).getSqlCommandType());
            return invocation.proceed();
        }
    }
}
