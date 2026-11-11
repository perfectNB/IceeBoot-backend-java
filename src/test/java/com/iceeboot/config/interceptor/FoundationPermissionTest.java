package com.iceeboot.config.interceptor;

import com.iceeboot.common.exception.GlobalExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.controller.agent.AgentConsoleController;
import com.iceeboot.framework.service.agent.IAgentConfigurationService;
import com.iceeboot.framework.service.agent.IAgentExecutionService;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.controller.pay.PayController;

import com.iceeboot.framework.service.pay.IPayConfigService;
import com.iceeboot.framework.service.pay.IPayRecordService;
import com.iceeboot.framework.service.system.IAuthCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FoundationPermissionTest {
    @Test void aiAndPaymentManagementCannotBypassConfiguredPermissions() throws Exception {
        var cache = mock(IAuthCacheService.class);
        var ai = mock(IAgentConfigurationService.class);
        var configs = mock(IPayConfigService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AgentConsoleController(ai, mock(IAgentExecutionService.class), mock(IAgentPersistenceService.class), mock(ISysUserService.class), new com.iceeboot.framework.service.agent.convert.AgentApiConverter(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()), mock(com.iceeboot.framework.service.agent.IAgentConversationService.class)),
                        new PayController(mock(IPayRecordService.class), configs))
                .addInterceptors(new PermissionInterceptor(cache))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        when(cache.getUrlPermissions(anyString(), anyString())).thenReturn(List.of("fixture:manage"));
        when(cache.getUserPermissions(7L)).thenReturn(List.of());
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(delete("/v1/agent-console/agents/1"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/v1/pay/configs")).andExpect(status().isForbidden());
            verifyNoInteractions(ai, configs);
        }
    }
    @Test void defaultConfigurationRequiresAiLoginAndExemptsOnlySignedPaymentCallback() throws Exception {
        var sources = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        var binder = new Binder(ConfigurationPropertySources.from(sources));
        var paths = binder.bind("iceeboot.auth.exclude-paths", Bindable.listOf(String.class)).get();
        assertThat(paths).doesNotContain("/v1/saa/**", "/iceeboot/api/v1/saa/**",
                "/v1/chat/ali", "/iceeboot/api/v1/chat/ali");
        assertThat(paths).contains("/v1/pay/callback", "/iceeboot/api/v1/pay/callback");
        assertThat(paths).doesNotContain("/v1/pay/**", "/v1/pay/configs");
    }
}
