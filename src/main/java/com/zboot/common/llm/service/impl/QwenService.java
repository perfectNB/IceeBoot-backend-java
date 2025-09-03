/**
 * 通义千问大模型服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-24
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
 *  通义千问大模型服务实现类
 * @author CodeIcee
 * @date 2025-09-03
 */

@Slf4j
public class QwenService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com";
    private static final String DEFAULT_MODEL = "qwen-turbo";
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.QWEN;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.warn("通义千问非流式聊天功能待实现");
        return Mono.just(createNotImplementedResponse());
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        log.warn("通义千问流式聊天功能待实现");
        return Flux.just(createNotImplementedResponse());
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        log.warn("通义千问API密钥验证功能待实现");
        return Mono.just(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "qwen-turbo",
                "qwen-plus",
                "qwen-max",
                "qwen-max-longcontext",
                "qwen2.5-72b-instruct"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        if (model != null && model.contains("longcontext")) {
            return 32768;
        }
        return 8192;
    }
    
    private LLMResponse createNotImplementedResponse() {
        return new LLMResponse()
                .setProvider(getProvider())
                .setError("功能待实现")
                .setFinished(true);
    }
}