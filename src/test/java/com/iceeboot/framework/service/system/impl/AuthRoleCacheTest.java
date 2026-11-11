package com.iceeboot.framework.service.system.impl;

import com.iceeboot.common.exception.AuthServiceUnavailableException;
import com.iceeboot.framework.entity.system.MenuDO;
import com.iceeboot.framework.entity.system.PermissionDO;
import com.iceeboot.framework.mapper.system.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 授权读取只调用当前资源查询，旧角色缓存不再是权限来源。 */
class AuthRoleCacheTest {
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final SysMenuMapper menus = mock(SysMenuMapper.class);
    private final SysPermissionMapper permissions = mock(SysPermissionMapper.class);
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysUserRoleMapper userRoles = mock(SysUserRoleMapper.class);
    private final SysRoleMenuMapper roleMenus = mock(SysRoleMenuMapper.class);
    private final SysRolePermissionMapper rolePermissions = mock(SysRolePermissionMapper.class);
    private final AuthCacheServiceImpl service = new AuthCacheServiceImpl(redis, menus, permissions,
            roles, userRoles, roleMenus, rolePermissions);

    @Test
    void readsCurrentPermissionsWithOneMapperCallAndNoRoleCacheRoundTrips() {
        var permission = new PermissionDO();
        permission.setId(20L); permission.setCode("example:read");
        when(permissions.selectAuthorizedByUserId(7L, "superAdmin")).thenReturn(List.of(permission));
        assertThat(service.getUserPermissions(7L)).extracting(value -> value.get("code"))
                .containsExactly("example:read");
        verify(permissions).selectAuthorizedByUserId(7L, "superAdmin");
        verifyNoMoreInteractions(permissions);
        verifyNoInteractions(redis, menus, roles, userRoles, roleMenus, rolePermissions);
    }

    @Test
    void readsCurrentMenusWithOneMapperCallAndNoRoleCacheRoundTrips() {
        var menu = new MenuDO();
        menu.setId(10L); menu.setTitle("根菜单"); menu.setParentId(0L);
        when(menus.selectAuthorizedByUserId(7L, "superAdmin")).thenReturn(List.of(menu));
        assertThat(service.getUserMenus(7L)).extracting(value -> value.get("id")).containsExactly(10L);
        verify(menus).selectAuthorizedByUserId(7L, "superAdmin");
        verifyNoMoreInteractions(menus);
        verifyNoInteractions(redis, permissions, roles, userRoles, roleMenus, rolePermissions);
    }

    @Test
    void missingUserDoesNotQueryOrReadOldCaches() {
        assertThat(service.getUserMenus(null)).isEmpty();
        assertThat(service.getUserPermissions(null)).isEmpty();
        verifyNoInteractions(redis, menus, permissions, roles, userRoles, roleMenus, rolePermissions);
    }

    @Test
    void databaseFailuresRejectWithoutFallingBackToCachedResources() {
        var failure = new QueryTimeoutException("fixture");
        when(menus.selectAuthorizedByUserId(7L, "superAdmin")).thenThrow(failure);
        when(permissions.selectAuthorizedByUserId(7L, "superAdmin")).thenThrow(failure);
        assertThatThrownBy(() -> service.getUserMenus(7L))
                .isInstanceOf(AuthServiceUnavailableException.class).hasCause(failure);
        assertThatThrownBy(() -> service.getUserPermissions(7L))
                .isInstanceOf(AuthServiceUnavailableException.class).hasCause(failure);
        verifyNoInteractions(redis, roles, userRoles, roleMenus, rolePermissions);
    }
}
