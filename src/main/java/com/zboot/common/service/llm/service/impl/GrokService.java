/**
 * Grok大模型服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.llm.service.impl;

import com.zboot.common.constant.SystemConstants;
import com.zboot.common.service.llm.dto.LLMRequest;
import com.zboot.common.service.llm.dto.LLMResponse;
import com.zboot.common.service.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Grok大模型服务实现类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class GrokService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://api.x.ai";
    private static final String DEFAULT_MODEL = "grok-beta";
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.GROK;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.warn("Grok非流式聊天功能待实现");
        return Mono.just(createNotImplementedResponse());
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        log.warn("Grok流式聊天功能待实现");
        return Flux.just(createNotImplementedResponse());
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        log.warn("Grok API密钥验证功能待实现");
        return Mono.just(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "grok-beta",
                "grok-vision-beta"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        return 8192;
    }
    
    private LLMResponse createNotImplementedResponse() {
        return new LLMResponse()
                .setProvider(getProvider())
                .setError("功能待实现")
                .setFinished(true);
    }
}