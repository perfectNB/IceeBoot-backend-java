/**
 * ChatGPT大模型服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.llm.service.impl;

import com.zboot.common.constant.SystemConstants;
import com.zboot.common.llm.dto.LLMRequest;
import com.zboot.common.llm.dto.LLMResponse;
import com.zboot.common.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * ChatGPT大模型服务实现类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class ChatGPTService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.CHATGPT;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        // TODO: 实现ChatGPT非流式聊天
        log.warn("ChatGPT非流式聊天功能待实现");
        return Mono.just(createNotImplementedResponse());
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        // TODO: 实现ChatGPT流式聊天
        log.warn("ChatGPT流式聊天功能待实现");
        return Flux.just(createNotImplementedResponse());
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        // TODO: 实现API密钥验证
        log.warn("ChatGPT API密钥验证功能待实现");
        return Mono.just(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "gpt-3.5-turbo",
                "gpt-4",
                "gpt-4-turbo",
                "gpt-4o",
                "gpt-4o-mini"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        if (model != null && model.contains("gpt-4")) {
            return 8192;
        }
        return 4096;
    }
    
    private LLMResponse createNotImplementedResponse() {
        return new LLMResponse()
                .setProvider(getProvider())
                .setError("功能待实现")
                .setFinished(true);
    }
}