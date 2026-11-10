package com.iceeboot.config.security;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.application.ApplicationInfo;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.iceeboot.common.exception.GlobalExceptionHandler;
import com.iceeboot.config.interceptor.PermissionInterceptor;
import com.iceeboot.framework.service.system.IAuthCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs the actual auth interceptor and exception advice without a live Redis or database. */
class SaTokenAuthenticationFailureMvcTest {
    private AnnotationConfigWebApplicationContext context;
    private SaTokenContext previousContext;
    private String previousRoutePrefix;
    private MockMvc mvc;
    private ProtectedController controller;

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
        controller = context.getBean(ProtectedController.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
        SaManager.setSaTokenContext(previousContext);
        ApplicationInfo.routePrefix = previousRoutePrefix;
    }

    @Test
    void validTokenAllowsTheProtectedRequest() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L);
            mvc.perform(get("/protected").header("Authorization", "Bearer current-token"))
                    .andExpect(status().isOk()).andExpect(content().string("protected"));
            assertThat(controller.calls.get()).isEqualTo(1);
        }
    }

    @Test
    void missingTokenStillReturns401WithoutRunningTheController() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            mvc.perform(get("/protected")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
            assertThat(controller.calls.get()).isZero();
            auth.verify(() -> StpUtil.getLoginIdByToken(anyString()), never());
        }
    }

    @Test
    void definitivelyInvalidTokenStillReturns401() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("invalid-token")).thenReturn(null);
            mvc.perform(get("/protected").header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
            assertThat(controller.calls.get()).isZero();
        }
    }

    @Test
    void explicitNotLoginFailureStillReturns401() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("expired-token"))
                    .thenThrow(new NotLoginException("expired", "token", "expired"));
            mvc.perform(get("/protected").header("Authorization", "Bearer expired-token"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
            assertThat(controller.calls.get()).isZero();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void storageOutageReturns503AndTheSameTokenWorksAfterRecovery(boolean timeout) throws Exception {
        RuntimeException failure = timeout ? new QueryTimeoutException("private backend detail")
                : new RedisConnectionFailureException("private backend detail");
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenThrow(failure).thenReturn(7L);
            var result = mvc.perform(get("/protected").header("Authorization", "Bearer current-token"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value(503))
                    .andExpect(jsonPath("$.message").value("登录验证服务暂不可用，请稍后重试"))
                    .andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("private backend detail");
            assertThat(controller.calls.get()).isZero();

            mvc.perform(get("/protected").header("Authorization", "Bearer current-token"))
                    .andExpect(status().isOk()).andExpect(content().string("protected"));
            assertThat(controller.calls.get()).isEqualTo(1);
            auth.verify(() -> StpUtil.logoutByTokenValue(anyString()), never());
        }
    }


    @Test
    void cookieResolverIsSharedWithAuthentication() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getTokenValue).thenReturn("cookie-token");
            auth.when(() -> StpUtil.getLoginIdByToken("cookie-token")).thenReturn(7L);
            mvc.perform(get("/protected")).andExpect(status().isOk());
        }
    }

    @Test
    void unverifiedAsyncSseDispatchStillRequiresAuthentication() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            mvc.perform(get("/protected").with(request -> { request.setDispatcherType(jakarta.servlet.DispatcherType.ASYNC); return request; })
                    .header("Accept", "text/event-stream, application/json")).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void authenticatedSseRedispatchDoesNotRejectAnAlreadyOpenStream() throws Exception {
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L);
            var first = mvc.perform(get("/protected").header("Accept", "text/event-stream")
                    .header("Authorization", "Bearer current-token")).andExpect(status().isOk()).andReturn();
            String marker = SaTokenConfig.class.getName() + ".authenticated";
            assertThat(first.getRequest().getAttribute(marker)).isEqualTo(Boolean.TRUE);
            mvc.perform(get("/protected").with(request -> { request.setDispatcherType(jakarta.servlet.DispatcherType.ASYNC); return request; })
                    .header("Accept", "text/event-stream").requestAttr(marker, first.getRequest().getAttribute(marker)))
                    .andExpect(status().isOk());
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({SaTokenConfig.class, PermissionInterceptor.class, GlobalExceptionHandler.class, ProtectedController.class})
    static class MvcConfiguration {
        @Bean IAuthCacheService authCacheService() { return mock(IAuthCacheService.class); }
    }

    @RestController
    static class ProtectedController {
        private final AtomicInteger calls = new AtomicInteger();

        @GetMapping("/protected")
        String protectedEndpoint() { calls.incrementAndGet(); return "protected"; }
    }
}
