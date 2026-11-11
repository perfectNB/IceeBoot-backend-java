package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.exception.AuthServiceUnavailableException;
import com.iceeboot.framework.entity.system.*;
import com.iceeboot.framework.mapper.system.*;
import com.iceeboot.framework.service.database.DatabaseInitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用真实 Mapper 验证管理员生效条件，避免仅用 Mockito 忽略 SQL 过滤条件。 */
class AuthSuperAdminAccessTest {
    private JdbcTemplate jdbc;
    private AuthCacheServiceImpl service;
    private RedisTemplate<String, Object> redis;
    private SysPermissionMapper permissionMapper;
    private final Map<String, Object> cache = new HashMap<>();
    private final QueryCounter queryCounter = new QueryCounter();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:super_admin_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (var type : List.of(UserDO.class, RoleDO.class, UserRoleDO.class, MenuDO.class, PermissionDO.class,
                RoleMenuDO.class, RolePermissionDO.class)) {
            String table = type.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class).value();
            String ddl = ReflectionTestUtils.invokeMethod(new DatabaseInitService(null),
                    "generateCreateTableSql", type, table);
            jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        }
        jdbc.update("INSERT INTO t_user(id,username,status,del_flag) VALUES(7,'fixture','active',0)");
        var configuration = new MybatisConfiguration();
        configuration.addInterceptor(queryCounter);
        for (var mapper : List.of(SysRoleMapper.class, SysUserRoleMapper.class, SysMenuMapper.class,
                SysPermissionMapper.class, SysRoleMenuMapper.class, SysRolePermissionMapper.class)) {
            configuration.addMapper(mapper);
        }
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        var sql = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        permissionMapper = sql.getMapper(SysPermissionMapper.class);
        redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), any());
        service = new AuthCacheServiceImpl(redis, sql.getMapper(SysMenuMapper.class),
                sql.getMapper(SysPermissionMapper.class), sql.getMapper(SysRoleMapper.class),
                sql.getMapper(SysUserRoleMapper.class), sql.getMapper(SysRoleMenuMapper.class),
                sql.getMapper(SysRolePermissionMapper.class));
        jdbc.update("INSERT INTO t_menu(id,title,name,path,parent_id,sort,status,del_flag) VALUES"
                + " (10,'根菜单','root','/root',0,1,'active',0),"
                + " (11,'新子菜单','new_child','/root/new',10,2,'active',0),"
                + " (12,'禁用菜单','disabled','/disabled',0,3,'inactive',0),"
                + " (13,'已删除菜单','deleted','/deleted',0,4,'active',1)");
        jdbc.update("INSERT INTO t_permission(id,name,code,type,status,sort,del_flag) VALUES"
                + " (20,'读取接口','example:read','api','active',1,0),"
                + " (21,'新增按钮','example:create','button','active',2,0),"
                + " (22,'菜单入口','example:menu','menu','active',3,0),"
                + " (23,'停用权限','example:disabled','api','inactive',4,0),"
                + " (24,'已删除权限','example:deleted','api','active',5,1)");
    }

    @Test
    void disabledOrDeletedAccountsLoseProtectedResourceAccessOnTheNextQuery() {
        assignRole("superAdmin");
        assertThat(service.hasUserPermissions(7L, List.of("example:read"), false)).isTrue();
        jdbc.update("UPDATE t_user SET status='inactive' WHERE id=7");
        assertThat(service.hasUserPermissions(7L, List.of("example:read"), false)).isFalse();
        assertThat(service.getUserPermissions(7L)).isEmpty();
        assertThat(service.getUserMenus(7L)).isEmpty();
        jdbc.update("UPDATE t_user SET status='active',del_flag=1 WHERE id=7");
        assertThat(service.hasUserPermissions(7L, List.of("example:read"), false)).isFalse();
        assertThat(service.getUserPermissions(7L)).isEmpty();
        assertThat(service.getUserMenus(7L)).isEmpty();
    }

    @Test
    void caseInsensitiveDatabaseMatchingDoesNotBroadenPermissionCodes() {
        assignRole("admin");
        jdbc.execute("ALTER TABLE t_permission ALTER COLUMN code VARCHAR_IGNORECASE(255)");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(101,42,20,0)");
        assertThat(service.hasUserPermissions(7L, List.of("example:read"), false)).isTrue();
        assertThat(service.hasUserPermissions(7L, List.of("EXAMPLE:READ"), false)).isFalse();
        assertThat(service.hasUserPermissions(7L, List.of("example:read", "EXAMPLE:READ"), true)).isFalse();
    }

    @Test
    void permissionServiceQueriesRespectUserAndRoleAssignments() {
        assignRole("admin");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(101,42,20,0)");
        var permissions = new SysPermissionServiceImpl(
                mock(com.iceeboot.framework.service.system.ISysRolePermissionService.class), redis);
        ReflectionTestUtils.setField(permissions, "baseMapper", permissionMapper);
        assertThat(permissions.getPermissionsByUserId(7L)).extracting(PermissionDO::getCode)
                .containsExactly("example:read");
        assertThat(permissions.getPermissionsByRoleId(42L)).extracting(PermissionDO::getCode)
                .containsExactly("example:read");
        assertThat(permissions.getPermissionsByUserId(8L)).isEmpty();
        assertThat(permissions.getPermissionsByRoleId(99L)).isEmpty();
        jdbc.update("UPDATE t_role SET code='superAdmin' WHERE id=42");
        assertThat(permissions.getPermissionsByRoleId(42L)).hasSize(3);
        jdbc.update("UPDATE t_role SET status='inactive' WHERE id=42");
        assertThat(permissions.getPermissionsByRoleId(42L)).isEmpty();
        assertThat(permissions.getPermissionsByUserId(7L)).isEmpty();
    }

    @Test
    void scopedChecksPreserveAnyAllSemanticsAndDoNotLoadThePermissionCatalog() {
        assignRole("superAdmin");
        queryCounter.calls.set(0);
        assertThat(service.hasUserPermissions(7L, List.of("example:read", "example:create"), true)).isTrue();
        assertThat(queryCounter.calls.get()).isEqualTo(1);
        assertThat(service.hasUserPermissions(7L, List.of("example:read", "missing"), false)).isTrue();
        assertThat(service.hasUserPermissions(7L, List.of("example:read", "missing"), true)).isFalse();
        assertThat(service.hasUserPermissions(7L, List.of("example:read", "example:read"), true)).isTrue();
        assertThat(service.hasUserPermissions(7L, List.of("example:disabled", "example:deleted"), false)).isFalse();
        assertThat(service.hasUserPermissions(7L, List.of(" ", ""), false)).isFalse();
        assertThat(service.hasUserPermissions(null, List.of("example:read"), false)).isFalse();
        verifyNoInteractions(redis);
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "deleted", "revoked", "permission", "assignment"})
    void scopedChecksRejectRevocationImmediatelyDespitePreviouslyGrantedAccess(String mode) {
        assignRole("admin");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(101,42,20,0)");
        assertThat(service.hasUserPermissions(7L, List.of("example:read"), false)).isTrue();
        switch (mode) {
            case "disabled" -> jdbc.update("UPDATE t_role SET status='inactive' WHERE id=42");
            case "deleted" -> jdbc.update("UPDATE t_role SET del_flag=1 WHERE id=42");
            case "revoked" -> jdbc.update("UPDATE t_user_role SET del_flag=1 WHERE user_id=7");
            case "permission" -> jdbc.update("UPDATE t_permission SET status='inactive' WHERE id=20");
            case "assignment" -> jdbc.update("UPDATE t_role_permission SET del_flag=1 WHERE id=101");
        }
        assertThat(service.hasUserPermissions(7L, List.of("example:read"), false)).isFalse();
    }

    @Test
    void scopedCheckDatabaseFailureFailsClosed() {
        assignRole("superAdmin");
        jdbc.execute("DROP TABLE t_role");
        assertThatThrownBy(() -> service.hasUserPermissions(7L, List.of("example:read"), false))
                .isInstanceOf(AuthServiceUnavailableException.class);
    }

    @Test
    void superAdminGetsNewActiveMenusAndPermissionsWithoutIndividualAssignments() {
        assignRole("superAdmin");
        var menus = service.getUserMenus(7L);
        assertThat(menus).extracting(menu -> menu.get("title")).containsExactly("根菜单");
        assertThat((List<?>) menus.getFirst().get("children")).hasSize(1);
        assertThat(service.getUserPermissions(7L)).extracting(permission -> permission.get("code"))
                .containsExactly("example:read", "example:create", "example:menu");

        jdbc.update("INSERT INTO t_menu(id,title,name,path,parent_id,sort,status,del_flag)"
                + " VALUES(30,'新增菜单','added','/added',0,5,'active',0)");
        jdbc.update("INSERT INTO t_permission(id,name,code,type,status,sort,del_flag)"
                + " VALUES(31,'新增接口','example:added','api','active',6,0)");
        assertThat(service.getUserMenus(7L)).extracting(menu -> menu.get("title"))
                .containsExactly("根菜单", "新增菜单");
        assertThat(service.getUserPermissions(7L)).extracting(permission -> permission.get("code"))
                .containsExactly("example:read", "example:create", "example:menu", "example:added")
                .doesNotContain("*");
    }

    @Test
    void ordinaryAdminRetainsOnlyExplicitAssignmentsEvenWithForgedCachedRoleName() {
        assignRole("admin");
        jdbc.update("INSERT INTO t_role_menu(id,role_id,menu_id,del_flag) VALUES(100,42,10,0)");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(101,42,20,0)");
        cache.put(CacheConstants.Auth.USER_ROLES_KEY + 7L,
                List.of(Map.of("id", 42L, "code", "superAdmin", "status", "active")));
        var menus = service.getUserMenus(7L);
        assertThat(menus).extracting(menu -> menu.get("title")).containsExactly("根菜单");
        assertThat(menus.getFirst().get("children")).isNull();
        assertThat(service.getUserPermissions(7L)).extracting(permission -> permission.get("code"))
                .containsExactly("example:read");
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "deleted", "revoked"})
    void invalidatedSuperAdminCannotRecoverFullAccessFromCachedOrExplicitGrants(String mode) {
        assignRole("superAdmin");
        jdbc.update("INSERT INTO t_role_menu(id,role_id,menu_id,del_flag) VALUES(100,42,10,0),(101,42,11,0)");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(102,42,20,0),(103,42,21,0)");
        service.getUserMenus(7L);
        service.getUserPermissions(7L);
        if (mode.equals("disabled")) jdbc.update("UPDATE t_role SET status='inactive' WHERE id=42");
        else if (mode.equals("deleted")) jdbc.update("UPDATE t_role SET del_flag=1 WHERE id=42");
        else jdbc.update("UPDATE t_user_role SET del_flag=1 WHERE user_id=7");
        assertThat(service.getUserMenus(7L)).isEmpty();
        assertThat(service.getUserPermissions(7L)).isEmpty();
    }

    @Test
    void eachResourceReadUsesOneCurrentDatabaseQueryWithoutRoleCacheAccess() {
        assignRole("superAdmin");
        queryCounter.calls.set(0);
        clearInvocations(redis);
        assertThat(service.getUserMenus(7L)).hasSize(1);
        assertThat(queryCounter.calls.get()).isEqualTo(1);
        assertThat(service.getUserPermissions(7L)).hasSize(3);
        assertThat(queryCounter.calls.get()).isEqualTo(2);
        verifyNoInteractions(redis);
    }

    @Test
    void revokedOrdinaryGrantsCannotBeRestoredByOldRoleCaches() {
        assignRole("admin");
        jdbc.update("INSERT INTO t_role_menu(id,role_id,menu_id,del_flag) VALUES(100,42,10,0)");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(101,42,20,0)");
        assertThat(service.getUserMenus(7L)).hasSize(1);
        assertThat(service.getUserPermissions(7L)).hasSize(1);
        jdbc.update("UPDATE t_role_menu SET del_flag=1 WHERE role_id=42");
        jdbc.update("UPDATE t_role_permission SET del_flag=1 WHERE role_id=42");
        assertThat(service.getUserMenus(7L)).isEmpty();
        assertThat(service.getUserPermissions(7L)).isEmpty();
        jdbc.update("UPDATE t_role_menu SET del_flag=0 WHERE role_id=42");
        jdbc.update("UPDATE t_role_permission SET del_flag=0 WHERE role_id=42");
        assertThat(service.getUserMenus(7L)).hasSize(1);
        assertThat(service.getUserPermissions(7L)).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "deleted"})
    void changedResourceStateIsEffectiveOnTheNextReadForOrdinaryRoles(String mode) {
        assignRole("admin");
        jdbc.update("INSERT INTO t_role_menu(id,role_id,menu_id,del_flag) VALUES(100,42,10,0)");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(101,42,20,0)");
        service.getUserMenus(7L);
        service.getUserPermissions(7L);
        String change = mode.equals("disabled") ? "status='inactive'" : "del_flag=1";
        jdbc.update("UPDATE t_menu SET " + change + " WHERE id=10");
        jdbc.update("UPDATE t_permission SET " + change + " WHERE id=20");
        assertThat(service.getUserMenus(7L)).isEmpty();
        assertThat(service.getUserPermissions(7L)).isEmpty();
    }

    @Test
    void forgedCachedResourcesCannotAddUnassignedPermissionsOrMenus() {
        assignRole("admin");
        jdbc.update("INSERT INTO t_role_menu(id,role_id,menu_id,del_flag) VALUES(100,42,10,0)");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(101,42,20,0)");
        var menu = new MenuDO();
        menu.setId(11L); menu.setTitle("伪造菜单"); menu.setParentId(0L); menu.setSort(1);
        var permission = new PermissionDO();
        permission.setId(21L); permission.setCode("example:create");
        cache.put(CacheConstants.Auth.ROLE_MENUS_KEY + 42L, List.of(menu));
        cache.put(CacheConstants.Auth.ROLE_PERMISSIONS_KEY + 42L, List.of(permission));
        assertThat(service.getUserMenus(7L)).extracting(value -> value.get("id")).containsExactly(10L);
        assertThat(service.getUserPermissions(7L)).extracting(value -> value.get("code"))
                .containsExactly("example:read");
    }

    @Test
    void duplicateGrantsFromMultipleActiveRolesReturnEachResourceOnce() {
        assignRole("admin");
        jdbc.update("INSERT INTO t_role(id,name,code,status,sort,del_flag) VALUES(43,'其他角色','other','active',2,0)");
        jdbc.update("INSERT INTO t_user_role(id,user_id,role_id,del_flag) VALUES(91,7,43,0),(92,7,43,0)");
        jdbc.update("INSERT INTO t_role_menu(id,role_id,menu_id,del_flag) VALUES(100,42,10,0),(101,43,10,0)");
        jdbc.update("INSERT INTO t_role_permission(id,role_id,permission_id,del_flag) VALUES(102,42,20,0),(103,43,20,0)");
        assertThat(service.getUserMenus(7L)).hasSize(1);
        assertThat(service.getUserPermissions(7L)).extracting(value -> value.get("code"))
                .containsExactly("example:read");
    }

    @Test
    void databaseFailureCannotFallBackToCachedSuperAdminGrants() {
        assignRole("superAdmin");
        service.getUserMenus(7L);
        service.getUserPermissions(7L);
        jdbc.execute("DROP TABLE t_role");
        assertThatThrownBy(() -> service.getUserMenus(7L)).isInstanceOf(AuthServiceUnavailableException.class);
        assertThatThrownBy(() -> service.getUserPermissions(7L)).isInstanceOf(AuthServiceUnavailableException.class);
    }

    @Test
    void resourceReadsDoNotDependOnRedisAfterAuthentication() {
        assignRole("superAdmin");
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("fixture"));
        assertThat(service.getUserMenus(7L)).hasSize(1);
        assertThat(service.getUserPermissions(7L)).hasSize(3);
    }

    @Intercepts(@Signature(type = Executor.class, method = "query", args = {
            MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
    }))
    static class QueryCounter implements Interceptor {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            calls.incrementAndGet();
            return invocation.proceed();
        }
    }

    private void assignRole(String code) {
        // 名称与账号均不决定超级权限，唯一依据是当前有效角色编码。
        jdbc.update("INSERT INTO t_role(id,name,code,status,sort,del_flag) VALUES(42,'超级管理员',?,'active',1,0)", code);
        jdbc.update("INSERT INTO t_user_role(id,user_id,role_id,del_flag) VALUES(90,7,42,0)");
    }
}
