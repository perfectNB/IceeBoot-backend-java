package com.iceeboot.framework.service.system.impl;

import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.framework.entity.system.*;
import com.iceeboot.framework.mapper.system.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import java.util.List;
import static org.mockito.Mockito.*;

class AuthRoleCacheTest {
    @Test void cachesRoleIdInsteadOfTheUserRoleRelationId() {
        RedisTemplate<String,Object> redis = mock(RedisTemplate.class);
        ValueOperations<String,Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var relations = mock(SysUserRoleMapper.class); var rolePermissions = mock(SysRolePermissionMapper.class);
        var row = new UserRoleDO(); row.setId(999L); row.setUserId(7L); row.setRoleId(42L);
        when(relations.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(row));
        when(rolePermissions.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        var service = new AuthCacheServiceImpl(redis, mock(SysMenuMapper.class), mock(SysPermissionMapper.class),
                mock(SysRoleMapper.class), relations, mock(SysRoleMenuMapper.class), rolePermissions);
        service.getUserPermissions(7L);
        verify(values).set(CacheConstants.Auth.USER_ROLE + 7L, List.of(42L));
        verify(values).get(CacheConstants.Auth.ROLE_PERMISSIONS_KEY + 42L);
        verify(values, never()).get(CacheConstants.Auth.ROLE_PERMISSIONS_KEY + 999L);
    }
}
