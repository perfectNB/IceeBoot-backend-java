/**
 * 百川智能大模型服务实现类
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
 * 百川智能大模型服务实现类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class BaichuanService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://api.baichuan-ai.com";
    private static final String DEFAULT_MODEL = "Baichuan2-Turbo";
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.BAICHUAN;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.warn("百川智能非流式聊天功能待实现");
        return Mono.just(createNotImplementedResponse());
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        log.warn("百川智能流式聊天功能待实现");
        return Flux.just(createNotImplementedResponse());
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        log.warn("百川智能API密钥验证功能待实现");
        return Mono.just(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "Baichuan2-Turbo",
                "Baichuan2-Turbo-192k",
                "Baichuan3-Turbo",
                "Baichuan3-Turbo-128k"
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
            if (model.contains("192k")) {
                return 192000;
            } else if (model.contains("128k")) {
                return 128000;
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