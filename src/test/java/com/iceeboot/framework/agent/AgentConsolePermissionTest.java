package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.exception.GlobalExceptionHandler;
import com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.config.interceptor.PermissionInterceptor;
import com.iceeboot.framework.controller.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.system.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.*;

class AgentConsolePermissionTest {
    @Test
    void everyEndpointUsesDynamicFailClosedPermission() {
        var endpoints =
                Arrays.stream(AgentConsoleController.class.getDeclaredMethods())
                        .filter(
                                m ->
                                        m.isAnnotationPresent(GetMapping.class)
                                                || m.isAnnotationPresent(PostMapping.class)
                                                || m.isAnnotationPresent(DeleteMapping.class))
                        .toList();
        assertThat(endpoints).hasSize(21);
        for (var method : endpoints) {
            RequirePermission permission = method.getAnnotation(RequirePermission.class);
            assertThat(permission).as(method.getName()).isNotNull();
            assertThat(permission.dynamic()).isTrue();
            assertThat(permission.code()).isBlank();
        }
    }

    @Test
    void missingPermissionCannotInvokeServiceEvenWhenNoDynamicMappingExists() throws Exception {
        var cache = mock(IAuthCacheService.class);
        var configs = mock(IAgentConfigurationService.class);
        var users = mock(ISysUserService.class);
        var controller =
                new AgentConsoleController(
                        configs,
                        mock(IAgentExecutionService.class),
                        mock(IAgentPersistenceService.class),
                        users,
                        new com.iceeboot.framework.service.agent.convert.AgentApiConverter(
                                new com.fasterxml.jackson.databind.ObjectMapper()
                                        .findAndRegisterModules()), mock(com.iceeboot.framework.service.agent.IAgentConversationService.class));
        var mvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .addInterceptors(new PermissionInterceptor(cache))
                        .setControllerAdvice(
                                new AgentConsoleExceptionHandler(), new GlobalExceptionHandler())
                        .build();
        when(cache.getUserPermissions(7L)).thenReturn(List.of());
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get("/v1/agent-console/models")).andExpect(status().isForbidden());
            verifyNoInteractions(configs, users);
        }
    }

    @Test
    void nonSuperAdminCannotUseConsoleEvenWithPermissionCode() throws Exception {
        var cache = mock(IAuthCacheService.class);
        var configs = mock(IAgentConfigurationService.class);
        var users = mock(ISysUserService.class);
        when(cache.getUserPermissions(7L)).thenReturn(List.of(Map.of("code", "ai:model:read")));
        when(users.getUserRoles(7L)).thenReturn(List.of("admin"));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new AgentConsoleController(
                                        configs,
                                        mock(IAgentExecutionService.class),
                                        mock(IAgentPersistenceService.class),
                                        users,
                                        new com.iceeboot.framework.service.agent.convert
                                                .AgentApiConverter(
                                                new com.fasterxml.jackson.databind.ObjectMapper()
                                                        .findAndRegisterModules()), mock(com.iceeboot.framework.service.agent.IAgentConversationService.class)))
                        .addInterceptors(new PermissionInterceptor(cache))
                        .setControllerAdvice(
                                new AgentConsoleExceptionHandler(), new GlobalExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get("/v1/agent-console/models")).andExpect(status().isForbidden());
            verifyNoInteractions(configs);
        }
    }

    @Test
    void superAdminWithPermissionCanReadMaskedConfiguration() throws Exception {
        var cache = mock(IAuthCacheService.class);
        var configs = mock(IAgentConfigurationService.class);
        var users = mock(ISysUserService.class);
        when(cache.getUserPermissions(1L)).thenReturn(List.of(Map.of("code", "ai:model:read")));
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        when(configs.models()).thenReturn(List.of());
        when(cache.getUrlPermissions(eq("/v1/agent-console/models"), eq("GET")))
                .thenReturn(List.of("ai:model:read"));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new AgentConsoleController(
                                        configs,
                                        mock(IAgentExecutionService.class),
                                        mock(IAgentPersistenceService.class),
                                        users,
                                        new com.iceeboot.framework.service.agent.convert
                                                .AgentApiConverter(
                                                new com.fasterxml.jackson.databind.ObjectMapper()
                                                        .findAndRegisterModules()), mock(com.iceeboot.framework.service.agent.IAgentConversationService.class)))
                        .addInterceptors(new PermissionInterceptor(cache))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            mvc.perform(get("/v1/agent-console/models")).andExpect(status().isOk());
            verify(configs).models();
        }
    }

    @Test
    void malformedJsonReturnsBadRequestWithoutCallingConfigurationService() throws Exception {
        var configs = mock(IAgentConfigurationService.class);
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new AgentConsoleController(
                                        configs,
                                        mock(IAgentExecutionService.class),
                                        mock(IAgentPersistenceService.class),
                                        mock(ISysUserService.class),
                                        new com.iceeboot.framework.service.agent.convert
                                                .AgentApiConverter(
                                                new com.fasterxml.jackson.databind.ObjectMapper()
                                                        .findAndRegisterModules()), mock(com.iceeboot.framework.service.agent.IAgentConversationService.class)))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        mvc.perform(
                        post("/v1/agent-console/models")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(configs);
    }

    @Test
    void readPermissionCannotDeleteTheSameParameterizedAgentPath() throws Exception {
        org.springframework.data.redis.core.RedisTemplate<String, Object> redis =
                mock(org.springframework.data.redis.core.RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> values =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(
                        com.iceeboot.common.constant.CacheConstants.Auth
                                .URL_PERMISSION_MAPPING_KEY))
                .thenReturn(
                        Map.of(
                                "GET /v1/agent-console/agents/{id}", List.of("ai:agent:read"),
                                "DELETE /v1/agent-console/agents/{id}", List.of("ai:agent:write")));
        var cache =
                spy(
                        new com.iceeboot.framework.service.system.impl.AuthCacheServiceImpl(
                                redis, null, null, null, null, null, null));
        doReturn(List.of(Map.of("code", "ai:agent:read"))).when(cache).getUserPermissions(7L);
        var configs = mock(IAgentConfigurationService.class);
        var store = mock(IAgentPersistenceService.class);
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(7L)).thenReturn(List.of("superAdmin"));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new AgentConsoleController(
                                        configs,
                                        mock(IAgentExecutionService.class),
                                        store,
                                        users,
                                        new com.iceeboot.framework.service.agent.convert
                                                .AgentApiConverter(
                                                new com.fasterxml.jackson.databind.ObjectMapper()), mock(com.iceeboot.framework.service.agent.IAgentConversationService.class)))
                        .addInterceptors(new PermissionInterceptor(cache))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get("/v1/agent-console/agents/42")).andExpect(status().isOk());
            mvc.perform(delete("/v1/agent-console/agents/42")).andExpect(status().isForbidden());
            verify(configs).agent(42L);
            verify(store, never()).deleteAgent(anyLong());
        }
    }
}
