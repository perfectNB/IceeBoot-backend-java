package com.iceeboot.config.security;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.application.ApplicationInfo;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.iceeboot.common.exception.GlobalExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.config.interceptor.PermissionInterceptor;
import com.iceeboot.framework.controller.system.AuthController;
import com.iceeboot.framework.controller.system.SysMenuController;
import com.iceeboot.framework.service.system.IAuthCacheService;
import com.iceeboot.framework.service.system.IAuthService;
import com.iceeboot.framework.service.system.ISysMenuService;
import com.iceeboot.framework.service.system.ISysRoleMenuService;
import com.iceeboot.framework.service.system.impl.SysMenuServiceImpl;
import com.iceeboot.framework.vo.system.SysUserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用真实登录、权限拦截链验证当前账号接口，不依赖数据库中的业务权限映射。 */
class CurrentAccountAccessMvcTest {
    private static final String CONTEXT_PATH = "/iceeboot/api";
    private AnnotationConfigWebApplicationContext context;
    private SaTokenContext previousContext;
    private String previousRoutePrefix;
    private MockMvc mvc;
    private IAuthCacheService cache;
    private ISysMenuService menus;
    private IAuthService authService;

    @BeforeEach
    void setUp() {
        previousContext = SaManager.getSaTokenContext();
        previousRoutePrefix = ApplicationInfo.routePrefix;
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        ApplicationInfo.routePrefix = "";
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(MvcConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        cache = context.getBean(IAuthCacheService.class);
        menus = context.getBean(ISysMenuService.class);
        authService = context.getBean(IAuthService.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
        SaManager.setSaTokenContext(previousContext);
        ApplicationInfo.routePrefix = previousRoutePrefix;
    }

    @Test
    void authenticatedUserCanLoadOwnMenusWithoutAnyApiPermissionMapping() throws Exception {
        when(menus.getUserMenu()).thenReturn(List.of());
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L);
            mvc.perform(accountRequest("/v1/menus/user").header("Authorization", "Bearer current-token"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result").isEmpty());
            verify(menus).getUserMenu();
            verifyNoInteractions(cache);
        }
    }

    @Test
    void authenticatedUserCanReadOwnProfileAndLogoutWithoutBusinessPermissions() throws Exception {
        var profile = new SysUserVO();
        profile.setId(7L);
        when(authService.getCurrentUser()).thenReturn(profile);
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L);
            mvc.perform(accountRequest("/v1/auth/user").header("Authorization", "Bearer current-token"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.id").value(7));
            mvc.perform(accountRequest("/v1/auth/logout").header("Authorization", "Bearer current-token"))
                    .andExpect(status().isOk());
            verify(authService).getCurrentUser();
            verify(authService).logout();
            verifyNoInteractions(cache);
        }
    }

    @Test
    void anonymousOrInvalidTokenCannotUseCurrentAccountEndpoints() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            for (String path : List.of("/v1/menus/user", "/v1/auth/user", "/v1/auth/logout")) {
                mvc.perform(accountRequest(path)).andExpect(status().isUnauthorized());
                mvc.perform(accountRequest(path).header("Authorization", "Bearer invalid-token"))
                        .andExpect(status().isUnauthorized());
            }
            verifyNoInteractions(cache, menus, authService);
        }
    }

    @Test
    void menuManagementStillRejectsMissingPermissionConfiguration() throws Exception {
        try (var auth = mockStatic(StpUtil.class); var tokens = mockStatic(TokenUtils.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L);
            tokens.when(() -> TokenUtils.resolveRequestToken(any())).thenReturn("current-token");
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get(CONTEXT_PATH + "/v1/menus/tree").contextPath(CONTEXT_PATH)
                            .header("Authorization", "Bearer current-token"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(CONTEXT_PATH + "/v1/menus").contextPath(CONTEXT_PATH)
                            .header("Authorization", "Bearer current-token")
                            .contentType("application/json").content("[1]"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(menus, authService);
        }
    }

    @Test
    void menuServiceReadsOnlyTheCurrentUsersAssignedMenus() {
        when(cache.getUserMenus(7L)).thenReturn(List.of(Map.of("id", 42L, "title", "个人菜单")));
        var service = new SysMenuServiceImpl(cache, mock(ISysRoleMenuService.class), null);
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            assertThat(service.getUserMenu()).singleElement().satisfies(menu -> {
                assertThat(menu.getId()).isEqualTo(42L);
                assertThat(menu.getTitle()).isEqualTo("个人菜单");
            });
            verify(cache).getUserMenus(7L);
            verifyNoMoreInteractions(cache);
        }
    }

    private MockHttpServletRequestBuilder accountRequest(String path) {
        var request = path.endsWith("/logout") ? post(CONTEXT_PATH + path) : get(CONTEXT_PATH + path);
        return request.contextPath(CONTEXT_PATH);
    }

    @Configuration
    @EnableWebMvc
    @Import({SaTokenConfig.class, PermissionInterceptor.class, GlobalExceptionHandler.class,
            SysMenuController.class, AuthController.class})
    static class MvcConfiguration {
        @Bean IAuthCacheService authCacheService() { return mock(IAuthCacheService.class); }
        @Bean ISysMenuService menuService() { return mock(ISysMenuService.class); }
        @Bean IAuthService authService() { return mock(IAuthService.class); }
    }
}
