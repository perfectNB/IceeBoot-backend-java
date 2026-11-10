package com.iceeboot.framework.service.system.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.common.utils.PasswordUtil;
import com.iceeboot.framework.dto.create.system.UserDTO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.mapper.system.SysUserMapper;
import com.iceeboot.framework.service.system.IAuthCacheService;
import com.iceeboot.framework.service.system.ISysRoleService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

class UserPasswordStorageTest {
    @Test
    void newUserStoresVerifiableHashWhileRetainingProfileAndRoles() {
        var mapper = mock(SysUserMapper.class);
        var roles = mock(ISysRoleService.class);
        var service =
                new SysUserServiceImpl(
                        mapper, roles, mock(IAuthCacheService.class), mock(RedisTemplate.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.insert(any(UserDO.class)))
                .thenAnswer(
                        invocation -> {
                            UserDO row = invocation.getArgument(0);
                            row.setId(42L);
                            return 1;
                        });
        var dto = new UserDTO();
        dto.setUsername("new-user");
        dto.setNickname("测试用户");
        dto.setPassword("review-only-password");
        dto.setEmail("user@example.invalid");
        dto.setRoleIds(List.of(3L));
        assertThat(service.addUser(dto)).isTrue();
        var saved = ArgumentCaptor.forClass(UserDO.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getPassword()).isNotEqualTo(dto.getPassword());
        assertThat(PasswordUtil.matches(dto.getPassword(), saved.getValue().getPassword()))
                .isTrue();
        assertThat(saved.getValue().getUsername()).isEqualTo(dto.getUsername());
        assertThat(saved.getValue().getNickname()).isEqualTo(dto.getNickname());
        assertThat(saved.getValue().getEmail()).isEqualTo(dto.getEmail());
        verify(roles).assignRolesToUser(42L, List.of(3L));
        assertThat(dto.getPassword()).isEqualTo("review-only-password");
    }
}
