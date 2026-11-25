package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.enums.agent.MemoryScope;
import com.iceeboot.framework.mapper.system.SysUserMapper;
import com.iceeboot.framework.query.agent.MemoryUserQuery;
import com.iceeboot.framework.service.agent.memory.*;
import com.iceeboot.framework.service.system.IAuthCacheService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import java.util.*;

class MemoryUserProfileServiceTest {
    SysUserMapper users = mock(SysUserMapper.class);
    IAuthCacheService auth = mock(IAuthCacheService.class);
    MemoryProfileService profiles = mock(MemoryProfileService.class);
    MemoryChangeService changes = mock(MemoryChangeService.class);
    MemoryUserProfileService service = new MemoryUserProfileService(users, auth, profiles, changes);

    @BeforeEach
    void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "memory-users-test"), UserDO.class);
        when(users.selectCount(any())).thenReturn(1L);
    }

    @Test void ordinaryPersonalPermissionsDoNotGrantCrossUserReadOrWrite() {
        when(auth.hasUserPermissions(eq(7L), anyList(), eq(true))).thenReturn(false);
        assertThatThrownBy(() -> service.profile(7, 8)).isInstanceOf(IceeBootException.class).hasMessage(ResponseMessage.Agent.PERMISSION_DENIED);
        assertThatThrownBy(() -> service.edit(7, 8, "city", "上海", 0)).isInstanceOf(IceeBootException.class);
        verifyNoInteractions(profiles, changes);
        verify(users, never()).selectCount(any());
    }

    @Test void everyManagementActionChecksItsOwnPermissionAndKeepsOwnerSeparateFromActor() {
        when(auth.hasUserPermissions(eq(1L), anyList(), eq(true))).thenReturn(true);
        service.profile(1, 7);
        service.edit(1, 7, "city", "上海", 2);
        service.forget(1, 7, "city", 2);
        service.pending(1, 7);
        service.review(1, 7, 88, true);
        verify(profiles).view(MemoryScope.USER, 0, "7");
        verify(profiles).editUser("7", "1", "city", "上海", 2);
        verify(profiles).editUser("7", "1", "city", null, 2);
        verify(changes).pending(MemoryScope.USER, 0, "7");
        verify(changes).reviewUser("7", "1", 88, true);
        for (String action : List.of("read", "edit", "forget", "review-read", "review"))
            verify(auth).hasUserPermissions(1L, List.of("ai:memory:user-" + action + ":api"), true);
    }

    @Test void deletedOrMissingUserCannotBeReadOrChanged() {
        when(auth.hasUserPermissions(eq(1L), anyList(), eq(true))).thenReturn(true);
        when(users.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.profile(1, 7)).hasMessage(ResponseMessage.Agent.MEMORY_USER_NOT_FOUND);
        assertThatThrownBy(() -> service.forget(1, 7, "city", 0)).hasMessage(ResponseMessage.Agent.MEMORY_USER_NOT_FOUND);
        verifyNoInteractions(profiles);
    }

    @Test void usersListIsPaginatedAndReturnsOnlyIdentitySummary() throws Exception {
        when(auth.hasUserPermissions(eq(1L), anyList(), eq(true))).thenReturn(true);
        UserDO user = new UserDO(); user.setId(9007199254740993L); user.setUsername("seven"); user.setNickname("测试用户"); user.setStatus("active");
        user.setPassword("not-for-response"); user.setEmail("private@example.invalid");
        var result = new Page<UserDO>(2, 20, 21); result.setRecords(List.of(user));
        when(users.selectPage(any(), any())).thenReturn(result);
        var query = new MemoryUserQuery(); query.setPageNum(2); query.setPageSize(20); query.setKeyword("测试");
        var response = service.users(1, query);
        assertThat(response.getTotal()).isEqualTo(21);
        assertThat(response.getRecords()).singleElement().satisfies(row -> assertThat(row.userId()).isEqualTo("9007199254740993"));
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
        assertThat(json).doesNotContain("not-for-response", "private@example.invalid", "password", "email");
        query.setPageSize(101);
        assertThatThrownBy(() -> service.users(1, query)).hasMessage(ResponseMessage.Agent.REQUEST_INVALID);
    }
}
