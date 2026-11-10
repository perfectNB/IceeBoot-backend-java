package com.iceeboot.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.iceeboot.agent.persistence.AgentStore;
import com.iceeboot.agent.service.*;
import com.iceeboot.agent.web.*;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.exception.GlobalExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.config.interceptor.PermissionInterceptor;
import com.iceeboot.framework.service.system.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class AgentConsolePermissionTest {
  @Test
  void everyEndpointUsesExplicitFailClosedPermission() {
    var endpoints =
        Arrays.stream(AgentConsoleController.class.getDeclaredMethods())
            .filter(
                m ->
                    m.isAnnotationPresent(GetMapping.class)
                        || m.isAnnotationPresent(PostMapping.class)
                        || m.isAnnotationPresent(DeleteMapping.class))
            .toList();
    assertThat(endpoints).hasSize(16);
    for (var method : endpoints) {
      RequirePermission permission = method.getAnnotation(RequirePermission.class);
      assertThat(permission).as(method.getName()).isNotNull();
      assertThat(permission.dynamic()).isFalse();
      assertThat(permission.code()).startsWith("ai:").isNotBlank();
    }
  }

  @Test
  void missingPermissionCannotInvokeServiceEvenWhenNoDynamicMappingExists() throws Exception {
    var cache = mock(IAuthCacheService.class);
    var configs = mock(AgentConfigurationService.class);
    var users = mock(ISysUserService.class);
    var controller =
        new AgentConsoleController(
            configs, mock(AgentExecutionService.class), mock(AgentStore.class), users);
    var mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .addInterceptors(new PermissionInterceptor(cache))
            .setControllerAdvice(new AgentConsoleExceptionHandler(), new GlobalExceptionHandler())
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
    var configs = mock(AgentConfigurationService.class);
    var users = mock(ISysUserService.class);
    when(cache.getUserPermissions(7L)).thenReturn(List.of(Map.of("code", "ai:model:read")));
    when(users.getUserRoles(7L)).thenReturn(List.of("admin"));
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AgentConsoleController(
                    configs, mock(AgentExecutionService.class), mock(AgentStore.class), users))
            .addInterceptors(new PermissionInterceptor(cache))
            .setControllerAdvice(new AgentConsoleExceptionHandler(), new GlobalExceptionHandler())
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
    var configs = mock(AgentConfigurationService.class);
    var users = mock(ISysUserService.class);
    when(cache.getUserPermissions(1L)).thenReturn(List.of(Map.of("code", "ai:model:read")));
    when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
    when(configs.models()).thenReturn(List.of());
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AgentConsoleController(
                    configs, mock(AgentExecutionService.class), mock(AgentStore.class), users))
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
    var configs = mock(AgentConfigurationService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AgentConsoleController(
                    configs,
                    mock(AgentExecutionService.class),
                    mock(AgentStore.class),
                    mock(ISysUserService.class)))
            .setControllerAdvice(new AgentConsoleExceptionHandler())
            .build();
    mvc.perform(
            post("/v1/agent-console/models")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(configs);
  }
}
