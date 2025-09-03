/**
 * 讯飞星火大模型服务实现类
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
 * 讯飞星火大模型服务实现类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class SparkService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://spark-api.xf-yun.com";
    private static final String DEFAULT_MODEL = "generalv3.5";
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.SPARK;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.warn("讯飞星火非流式聊天功能待实现");
        return Mono.just(createNotImplementedResponse());
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        log.warn("讯飞星火流式聊天功能待实现");
        return Flux.just(createNotImplementedResponse());
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        log.warn("讯飞星火API密钥验证功能待实现");
        return Mono.just(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "generalv3.5",
                "generalv3",
                "generalv2",
                "pro-128k",
                "max-32k"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        if (model != null) {
            if (model.contains("128k")) {
                return 128000;
            } else if (model.contains("32k")) {
                return 32000;
            }
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