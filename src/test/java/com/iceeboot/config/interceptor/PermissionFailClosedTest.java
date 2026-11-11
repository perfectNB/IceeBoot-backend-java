package com.iceeboot.config.interceptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.exception.GlobalExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.system.IAuthCacheService;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.*;

class PermissionFailClosedTest {
    @RestController
    static class Endpoint {
        int calls;

        @GetMapping("/dynamic")
        @RequirePermission(dynamic = true)
        public String dynamic() {
            calls++;
            return "ok";
        }

        @GetMapping("/missing-static")
        @RequirePermission
        public String missingStatic() {
            calls++;
            return "ok";
        }

        @GetMapping("/plain")
        public String plain() {
            calls++;
            return "ok";
        }
    }

    @Test
    void nullEmptyAndBlankDynamicMappingsRejectLoggedInUserWithoutCallingController()
            throws Exception {
        for (List<String> mapping :
                Arrays.<List<String>>asList(null, List.of(), List.of("", " "))) {
            var endpoint = new Endpoint();
            var cache = mock(IAuthCacheService.class);
            when(cache.getUrlPermissions(eq("/dynamic"), anyString())).thenReturn(mapping);
            var mvc =
                    MockMvcBuilders.standaloneSetup(endpoint)
                            .addInterceptors(new PermissionInterceptor(cache))
                            .setControllerAdvice(new GlobalExceptionHandler())
                            .build();
            try (var tokens = mockStatic(TokenUtils.class)) {
                tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
                mvc.perform(get("/dynamic")).andExpect(status().isForbidden());
                assertThat(endpoint.calls).isZero();
                verify(cache, never()).getUserPermissions(anyLong());
                verify(cache, never()).hasUserPermissions(any(), anyList(), anyBoolean());
            }
        }
    }

    @Test
    void configuredPermissionAllowsOnlyUserWhoOwnsIt() throws Exception {
        var endpoint = new Endpoint();
        var cache = mock(IAuthCacheService.class);
        when(cache.getUrlPermissions(eq("/dynamic"), anyString()))
                .thenReturn(List.of("", "sample:read", "sample:read"));
        when(cache.hasUserPermissions(eq(7L), anyList(), anyBoolean())).thenReturn(true);
        var mvc =
                MockMvcBuilders.standaloneSetup(endpoint)
                        .addInterceptors(new PermissionInterceptor(cache))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get("/dynamic")).andExpect(status().isOk());
            when(cache.hasUserPermissions(eq(7L), anyList(), anyBoolean())).thenReturn(false);
            mvc.perform(get("/dynamic")).andExpect(status().isForbidden());
            assertThat(endpoint.calls).isEqualTo(1);
            verify(cache, times(2)).hasUserPermissions(7L, List.of("sample:read"), false);
            verify(cache, never()).getUserPermissions(anyLong());
        }
    }

    @Test
    void emptyStaticAnnotationRejectsAccessWhileUnannotatedRouteKeepsItsPolicy() throws Exception {
        var endpoint = new Endpoint();
        var cache = mock(IAuthCacheService.class);
        var mvc =
                MockMvcBuilders.standaloneSetup(endpoint)
                        .addInterceptors(new PermissionInterceptor(cache))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get("/missing-static")).andExpect(status().isForbidden());
            mvc.perform(get("/plain")).andExpect(status().isOk());
            assertThat(endpoint.calls).isEqualTo(1);
        }
    }
}
