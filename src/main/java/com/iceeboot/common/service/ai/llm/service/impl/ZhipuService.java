package com.iceeboot.common.service.ai.llm.service.impl;

import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 智谱AI大模型服务实现类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class ZhipuService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn";
    private static final String DEFAULT_MODEL = "glm-4";
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.ZHIPU;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.warn("智谱AI非流式聊天功能待实现");
        return Mono.just(createNotImplementedResponse());
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        log.warn("智谱AI流式聊天功能待实现");
        return Flux.just(createNotImplementedResponse());
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        log.warn("智谱AI API密钥验证功能待实现");
        return Mono.just(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "glm-4",
                "glm-4v",
                "glm-3-turbo",
                "chatglm3-6b"
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