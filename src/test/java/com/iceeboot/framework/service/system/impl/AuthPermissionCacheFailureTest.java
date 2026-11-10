package com.iceeboot.framework.service.system.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.exception.AuthServiceUnavailableException;
import com.iceeboot.framework.mapper.system.SysPermissionMapper;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthPermissionCacheFailureTest {
    @Test
    void databaseFailureCannotCreateAndCacheExamplePermissions() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var mapper = mock(SysPermissionMapper.class);
        var failure = new DataAccessResourceFailureException("database unavailable");
        when(mapper.selectList(any())).thenThrow(failure);
        var service = new AuthCacheServiceImpl(redis, null, mapper, null, null, null, null);
        assertThatThrownBy(service::getAllUrlPermissions)
                .isInstanceOf(AuthServiceUnavailableException.class)
                .hasCause(failure);
        verify(values, never()).set(eq(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY), any());
    }

    @Test
    void startupInvalidatesLegacyUrlMappingBeforeLoadingRoles() throws Exception {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        var roles = mock(com.iceeboot.framework.service.system.ISysRoleService.class);
        when(roles.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of());
        var startup =
                new RoleCacheInitService(
                        roles,
                        mock(com.iceeboot.framework.service.system.ISysRolePermissionService.class),
                        mock(com.iceeboot.framework.service.system.ISysRoleMenuService.class),
                        mock(com.iceeboot.framework.service.system.ISysPermissionService.class),
                        mock(com.iceeboot.framework.service.system.ISysMenuService.class),
                        redis);
        startup.run(new org.springframework.boot.DefaultApplicationArguments());
        var order = inOrder(redis, roles);
        order.verify(redis).delete(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY);
        order.verify(roles).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}
