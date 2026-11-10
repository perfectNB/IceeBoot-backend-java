package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.controller.agent.AgentConsoleController;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.service.agent.impl.AgentExecutionServiceImpl;
import com.iceeboot.framework.service.agent.runtime.AgentModelFactory;
import com.iceeboot.framework.service.agent.runtime.HarnessRuntimeFactory;
import com.iceeboot.framework.service.agent.runtime.SchemaSupport;
import com.iceeboot.framework.service.system.ISysUserService;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

class AgentExceptionHandlingTest {
    @Test
    void modelProtocolValidationUsesFrameworkExceptionAndMessageConstant() throws Exception {
        var f = new AgentFixture();
        assertThatThrownBy(
                        () ->
                                f.configs.saveModel(
                                        new ModelConfig(
                                                null,
                                                "test",
                                                Provider.CLAUDE,
                                                Protocol.OPENAI_CHAT,
                                                "https://example.invalid",
                                                "key",
                                                "model",
                                                4096,
                                                true,
                                                null)))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.MODEL_PROVIDER_PROTOCOL_MISMATCH);
    }

    @Test
    void sdkModelFailureIsWrappedWithSafeConstantAndOriginalCause() {
        var store = mock(IAgentPersistenceService.class);
        var models = mock(AgentModelFactory.class);
        var model =
                new ModelConfig(
                        1L,
                        "test",
                        Provider.GPT,
                        Protocol.OPENAI_CHAT,
                        "https://example.invalid",
                        "enc:key",
                        "test-model",
                        4096,
                        true,
                        null);
        when(store.model(1L)).thenReturn(model);
        var original =
                new IllegalArgumentException("third-party request api_key=never-return-this");
        when(models.create(model, null)).thenThrow(original);
        var service =
                new AgentExecutionServiceImpl(
                        store,
                        mock(HarnessRuntimeFactory.class),
                        models,
                        new AgentProperties(),
                        new ObjectMapper());
        assertThatThrownBy(() -> service.invokeModel(new ModelInvokeRequest(1L, "input", null)))
                .isInstanceOfSatisfying(
                        IceeBootException.class,
                        e -> {
                            assertThat(e.getCode()).isEqualTo(ResultCodeEnum.ERROR.getCode());
                            assertThat(e.getMessage())
                                    .isEqualTo(ResponseMessage.Agent.MODEL_INVOKE_FAILED);
                            assertThat(e.getCause()).isSameAs(original);
                        });
    }

    @Test
    void unresolvedSchemaReferenceUsesFrameworkExceptionInsteadOfLeakingCompilerError() {
        assertThatThrownBy(() -> SchemaSupport.validate(Map.of("$ref", "#/missing"), Map.of()))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_INVALID)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void businessExceptionReturnsSafeMessageAndPreservesStatusWithoutCauseDetails()
            throws Exception {
        var configs = mock(IAgentConfigurationService.class);
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        when(configs.models())
                .thenThrow(
                        new IceeBootException(
                                ResponseMessage.Agent.MODEL_DISABLED,
                                new IllegalStateException("api_key=never-return-this")));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new AgentConsoleController(
                                        configs,
                                        mock(IAgentExecutionService.class),
                                        mock(IAgentPersistenceService.class),
                                        users,
                                        new AgentApiConverter(new ObjectMapper())))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            mvc.perform(get("/v1/agent-console/models"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(ResponseMessage.Agent.MODEL_DISABLED))
                    .andExpect(
                            content()
                                    .string(
                                            org.hamcrest.Matchers.not(
                                                    org.hamcrest.Matchers.containsString(
                                                            "never-return-this"))));
        }
    }

    @Test
    void unexpectedSdkArgumentExceptionDoesNotExposeItsMessageToHttpClient() throws Exception {
        var configs = mock(IAgentConfigurationService.class);
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        when(configs.models()).thenThrow(new IllegalArgumentException("api_key=never-return-this"));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new AgentConsoleController(
                                        configs,
                                        mock(IAgentExecutionService.class),
                                        mock(IAgentPersistenceService.class),
                                        users,
                                        new AgentApiConverter(new ObjectMapper())))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            mvc.perform(get("/v1/agent-console/models"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(ResponseMessage.Agent.OPERATION_FAILED))
                    .andExpect(
                            content()
                                    .string(
                                            org.hamcrest.Matchers.not(
                                                    org.hamcrest.Matchers.containsString(
                                                            "never-return-this"))));
        }
    }
}
