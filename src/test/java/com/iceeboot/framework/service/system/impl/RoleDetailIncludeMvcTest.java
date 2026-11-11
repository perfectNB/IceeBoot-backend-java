package com.iceeboot.framework.service.system.impl;

import com.iceeboot.common.exception.GlobalExceptionHandler;
import com.iceeboot.framework.controller.system.SysRoleController;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.mapper.system.SysRoleMapper;
import com.iceeboot.framework.service.system.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** 原路由和默认响应保持兼容，弹框可仅请求所需权限树或菜单树。 */
class RoleDetailIncludeMvcTest {
    ISysPermissionService permissions;
    ISysMenuService menus;
    SysRoleMapper roles;
    MockMvc mvc;
    @BeforeEach void setup() {
        permissions=mock(ISysPermissionService.class); menus=mock(ISysMenuService.class);
        roles=mock(SysRoleMapper.class);
        var role=new RoleDO(); role.setId(7L); role.setCode("fixture");
        when(roles.selectById(7L)).thenReturn(role);
        when(permissions.getPermissionTreeByRoleId(7L)).thenReturn(List.of());
        when(menus.getMenuTreeByRoleId(7L)).thenReturn(List.of());
        var service=new SysRoleServiceImpl(mock(ISysRolePermissionService.class),permissions,
                mock(RedisTemplate.class),mock(ISysUserRoleService.class),menus,
                mock(ISysRoleMenuService.class),mock(TransactionTemplate.class));
        ReflectionTestUtils.setField(service,"baseMapper",roles);
        mvc=standaloneSetup(new SysRoleController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void permissionsDialogDoesNotLoadMenus() throws Exception {
        mvc.perform(get("/v1/roles/7").param("include","permissions"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.permissionTree").isArray())
                .andExpect(jsonPath("$.result.menuTree").doesNotExist());
        verify(permissions).getPermissionTreeByRoleId(7L); verifyNoInteractions(menus);
    }
    @Test void menusDialogDoesNotLoadPermissions() throws Exception {
        mvc.perform(get("/v1/roles/7").param("include","menus"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.menuTree").isArray())
                .andExpect(jsonPath("$.result.permissionTree").doesNotExist());
        verify(menus).getMenuTreeByRoleId(7L); verifyNoInteractions(permissions);
    }
    @Test void existingRequestStillLoadsBothTrees() throws Exception {
        mvc.perform(get("/v1/roles/7")).andExpect(jsonPath("$.code").value(200));
        verify(menus).getMenuTreeByRoleId(7L); verify(permissions).getPermissionTreeByRoleId(7L);
    }
    @Test void invalidScopeDoesNotQueryRoleOrAnyTree() throws Exception {
        mvc.perform(get("/v1/roles/7").param("include","everything"))
                .andExpect(jsonPath("$.code").value(500));
        verifyNoInteractions(roles,permissions,menus);
    }
}
