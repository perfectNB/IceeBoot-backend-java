package com.iceeboot.framework.service.ai.impl;

import com.iceeboot.common.service.ai.llm.factory.LLMFactory;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import com.iceeboot.framework.entity.ai.*;
import com.iceeboot.framework.dto.create.ai.StreamChatRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LlmEndpointConfigurationTest {
    @Test void chatUsesTheStoredEndpointAndSelectedModel() {
        var service = mock(AiChatServiceImpl.class, CALLS_REAL_METHODS);
        var session = new AiChatSessionDO(); session.setModelName("custom-model");
        var key = new AiLLMKeyDO(); key.setApiKey("test-secret"); key.setUrl(" https://gateway.example/v1 ");
        var request = service.buildLLMRequest(new StreamChatRequest().setContent("hello"), session, key);
        assertThat(request.getBaseUrl()).isEqualTo("https://gateway.example/v1");
        assertThat(request.getModel()).isEqualTo("custom-model");
        assertThat(request.getApiKey()).isEqualTo("test-secret");
    }
    @Test void validationUsesTheSameStoredEndpoint() {
        var factory = mock(LLMFactory.class); var model = mock(LLMService.class);
        var service = spy(new AiLLMKeyServiceImpl(factory));
        var key = new AiLLMKeyDO(); key.setLlmProvider("deepseek"); key.setApiKey("test-secret"); key.setUrl(" https://gateway.example/v1 ");
        doReturn(key).when(service).getById(1L);
        when(factory.getService("deepseek")).thenReturn(model);
        when(model.validateApiKey("test-secret", "https://gateway.example/v1")).thenReturn(Mono.just(true));
        assertThat(service.validateLLMKey(1L)).isTrue();
        verify(model).validateApiKey("test-secret", "https://gateway.example/v1");
    }
    @Test void absentEndpointRetainsTheProviderDefaultContract() {
        var service = mock(AiChatServiceImpl.class, CALLS_REAL_METHODS);
        var key = new AiLLMKeyDO(); key.setUrl("  ");
        assertThat(service.buildLLMRequest(new StreamChatRequest(), new AiChatSessionDO(), key).getBaseUrl()).isNull();
    }
}
