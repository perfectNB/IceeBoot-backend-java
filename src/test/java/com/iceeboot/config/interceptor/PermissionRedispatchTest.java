package com.iceeboot.config.interceptor;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.system.IAuthCacheService;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionRedispatchTest {
    static class Endpoint {
        @RequirePermission("sample:read") public void read() {}
        @RequirePermission(dynamic = true) public void dynamicRead() {}
    }
    @Test void onlyAnAlreadyAuthorizedStreamMaySkipRedispatchChecks() throws Exception {
        var cache = mock(IAuthCacheService.class);
        var interceptor = new PermissionInterceptor(cache);
        var handler = new HandlerMethod(new Endpoint(), Endpoint.class.getMethod("read"));
        var request = new MockHttpServletRequest("GET", "/sample");
        request.addHeader("Accept", "text/event-stream");
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            when(cache.getUserPermissions(7L)).thenReturn(List.of(Map.of("code", "sample:read")));
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(null);
            request.setDispatcherType(DispatcherType.ASYNC);
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
            var unverified = new MockHttpServletRequest("GET", "/sample");
            unverified.addHeader("Accept", "text/event-stream");
            unverified.setDispatcherType(DispatcherType.ASYNC);
            assertThatThrownBy(() -> interceptor.preHandle(unverified, new MockHttpServletResponse(), handler))
                    .isInstanceOf(cn.dev33.satoken.exception.NotPermissionException.class);
        }
    }
    @Test void authorizedDynamicStreamMayResumeAfterTokenRotation() throws Exception {
        var cache = mock(IAuthCacheService.class);
        when(cache.getUrlPermissions(eq("/sample"), anyString())).thenReturn(List.of("sample:read"));
        when(cache.getUserPermissions(7L)).thenReturn(List.of(Map.of("code", "sample:read")));
        var interceptor = new PermissionInterceptor(cache);
        var handler = new HandlerMethod(new Endpoint(), Endpoint.class.getMethod("dynamicRead"));
        var request = new MockHttpServletRequest("GET", "/sample");
        request.addHeader("Accept", "text/event-stream");
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(null);
            request.setDispatcherType(DispatcherType.ASYNC);
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
        }
    }
    @Test void missingDynamicMappingCannotAuthorizeAsyncRedispatch() throws Exception {
        var cache = mock(IAuthCacheService.class);
        when(cache.getUrlPermissions(eq("/sample"), anyString())).thenReturn(List.of());
        var interceptor = new PermissionInterceptor(cache);
        var handler = new HandlerMethod(new Endpoint(), Endpoint.class.getMethod("dynamicRead"));
        var request = new MockHttpServletRequest("GET", "/sample");
        request.addHeader("Accept", "text/event-stream");
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                    .isInstanceOf(cn.dev33.satoken.exception.NotPermissionException.class);
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(null);
            request.setDispatcherType(DispatcherType.ASYNC);
            assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                    .isInstanceOf(cn.dev33.satoken.exception.NotPermissionException.class);
        }
    }
}
