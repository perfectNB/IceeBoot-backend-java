package com.iceeboot.framework.service.system.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.framework.entity.system.PermissionDO;
import com.iceeboot.framework.mapper.system.SysPermissionMapper;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

class AuthUrlPermissionTest {
    private static AuthCacheServiceImpl service(Map<String, List<String>> mapping) {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var stored = new AtomicReference<Object>(mapping);
        when(values.get(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY))
                .thenAnswer(call -> stored.get());
        doAnswer(
                        call -> {
                            stored.set(call.getArgument(1));
                            return null;
                        })
                .when(values)
                .set(eq(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY), any());
        return new AuthCacheServiceImpl(
                redis, null, mock(SysPermissionMapper.class), null, null, null, null);
    }

    @Test
    void sameUrlUsesDifferentPermissionsForDifferentMethods() {
        var cache =
                service(
                        Map.of(
                                "GET /v1/models",
                                List.of("model:read"),
                                "POST /v1/models",
                                List.of("model:write")));
        assertThat(cache.getUrlPermissions("/v1/models", "GET")).containsExactly("model:read");
        assertThat(cache.getUrlPermissions("/v1/models", "POST")).containsExactly("model:write");
        assertThat(cache.getUrlPermissions("/v1/models", "DELETE")).isEmpty();
        assertThat(cache.getUrlPermissions("/v1/models"))
                .containsExactlyInAnyOrder("model:read", "model:write");
    }

    @Test
    void pathParametersResolveForDetailsPublishingAndCodeInvocation() {
        var cache =
                service(
                        Map.of(
                                "GET /v1/agents/{id}",
                                List.of("agent:read"),
                                "POST /v1/agents/{id}/publish",
                                List.of("agent:publish"),
                                "POST /v1/agents/by-code/{code}/invoke",
                                List.of("agent:invoke")));
        assertThat(cache.getUrlPermissions("/v1/agents/123", "GET")).containsExactly("agent:read");
        assertThat(cache.getUrlPermissions("/v1/agents/123/publish", "POST"))
                .containsExactly("agent:publish");
        assertThat(cache.getUrlPermissions("/v1/agents/by-code/my_agent/invoke", "POST"))
                .containsExactly("agent:invoke");
        assertThat(cache.getUrlPermissions("/v1/agents/123/publish", "GET")).isEmpty();
    }

    @Test
    void mostSpecificPathAndThenMethodWinWithoutPrefixOvermatching() {
        var cache =
                service(
                        Map.of(
                                "GET /v1/agents/**",
                                List.of("broad"),
                                "GET /v1/agents/{id}",
                                List.of("specific"),
                                "* /v1/legacy/**",
                                List.of("legacy"),
                                "GET /v1/legacy/**",
                                List.of("legacy:read")));
        assertThat(cache.getUrlPermissions("/v1/agents/1", "GET")).containsExactly("specific");
        assertThat(cache.getUrlPermissions("/v1/agents-elsewhere/1", "GET")).isEmpty();
        assertThat(cache.getUrlPermissions("/v1/legacy/a", "POST")).containsExactly("legacy");
        assertThat(cache.getUrlPermissions("/v1/legacy/a", "GET")).containsExactly("legacy:read");
    }

    @Test
    void oldUrlOnlyCacheIsRebuiltFromMethodAwareDatabaseRows() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var value = new AtomicReference<Object>(Map.of("/v1/models", List.of("stale:grant")));
        when(values.get(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY))
                .thenAnswer(call -> value.get());
        doAnswer(
                        call -> {
                            value.set(call.getArgument(1));
                            return null;
                        })
                .when(values)
                .set(eq(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY), any());
        var read = new PermissionDO();
        read.setPath("/v1/models");
        read.setCode("model:read");
        read.setMethod("GET");
        var write = new PermissionDO();
        write.setPath("/v1/models");
        write.setCode("model:write");
        write.setMethod("POST, PUT");
        var mapper = mock(SysPermissionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(read, write));
        var cache = new AuthCacheServiceImpl(redis, null, mapper, null, null, null, null);
        assertThat(cache.getUrlPermissions("/v1/models", "GET")).containsExactly("model:read");
        assertThat(cache.getUrlPermissions("/v1/models", "POST")).containsExactly("model:write");
        assertThat(cache.getUrlPermissions("/v1/models", "PUT")).containsExactly("model:write");
        assertThat(cache.getUrlPermissions("/v1/models", "DELETE")).isEmpty();
        verify(mapper, times(1)).selectList(any());
    }

    @Test
    void legacySetterReplacesEveryMethodAtThatPathWithoutTouchingOtherPaths() {
        var cache =
                service(
                        Map.of(
                                "GET /v1/models",
                                List.of("read"),
                                "POST /v1/models",
                                List.of("write"),
                                "GET /v1/other",
                                List.of("other")));
        cache.setUrlPermissions("/v1/models", List.of("legacy"));
        assertThat(cache.getUrlPermissions("/v1/models", "GET")).containsExactly("legacy");
        assertThat(cache.getUrlPermissions("/v1/models", "POST")).containsExactly("legacy");
        cache.setUrlPermissions("/v1/models", List.of());
        assertThat(cache.getUrlPermissions("/v1/models", "GET")).isEmpty();
        assertThat(cache.getUrlPermissions("/v1/other", "GET")).containsExactly("other");
    }
}
