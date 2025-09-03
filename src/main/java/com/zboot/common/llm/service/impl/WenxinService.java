/**
 * 文心一言大模型服务实现类
 * 百度文心一言使用OAuth2.0认证方式，与其他厂商不同
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
 * 文心一言大模型服务实现类
 * 注意：文心一言使用OAuth2.0认证，需要先获取access_token
 */
@Slf4j
public class WenxinService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://aip.baidubce.com";
    private static final String DEFAULT_MODEL = "ernie-bot-turbo";
    private static final String TOKEN_ENDPOINT = "/oauth/2.0/token";
    private static final String CHAT_ENDPOINT = "/rpc/2.0/ai/v1/chat/completions";
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.WENXIN;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        // TODO: 实现文心一言非流式聊天
        // 需要先通过API Key和Secret Key获取access_token
        // 然后使用access_token调用聊天接口
        log.warn("文心一言非流式聊天功能待实现");
        return Mono.just(createNotImplementedResponse());
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        // TODO: 实现文心一言流式聊天
        // 文心一言的流式输出格式可能与OpenAI格式有所不同
        log.warn("文心一言流式聊天功能待实现");
        return Flux.just(createNotImplementedResponse());
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        // TODO: 实现API密钥验证
        // 文心一言需要验证API Key和Secret Key组合
        log.warn("文心一言API密钥验证功能待实现");
        return Mono.just(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "ernie-bot",
                "ernie-bot-turbo",
                "ernie-bot-4",
                "ernie-3.5",
                "ernie-speed",
                "ernie-lite"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        if (model != null && model.contains("ernie-bot-4")) {
            return 8192;
        }
        return 4096;
    }
    
    @Override
    public boolean supportsSystemPrompt() {
        // 文心一言支持系统提示词，但格式可能不同
        return true;
    }
    
    /**
     * 获取访问令牌
     * 文心一言特有的OAuth2.0认证流程
     * @param apiKey API Key
     * @param secretKey Secret Key
     * @return 访问令牌
     */
    private Mono<String> getAccessToken(String apiKey, String secretKey) {
        // TODO: 实现OAuth2.0令牌获取
        log.warn("文心一言访问令牌获取功能待实现");
        return Mono.just("");
    }
    
    private LLMResponse createNotImplementedResponse() {
        return new LLMResponse()
                .setProvider(getProvider())
                .setError("功能待实现 - 文心一言使用特殊的OAuth2.0认证方式")
                .setFinished(true);
    }
}