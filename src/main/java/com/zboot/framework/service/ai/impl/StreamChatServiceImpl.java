package com.zboot.framework.service.ai.impl;

import com.zboot.common.service.llm.dto.LLMRequest;
import com.zboot.common.service.llm.dto.LLMResponse;
import com.zboot.common.service.llm.factory.LLMFactory;
import com.zboot.common.service.llm.service.LLMService;
import com.zboot.framework.dto.create.ai.StreamChatRequest;
import com.zboot.framework.entity.ai.AiChatSessionDO;
import com.zboot.framework.entity.ai.AiLLMKeyDO;
import com.zboot.framework.service.ai.IAiChatService;
import com.zboot.framework.service.ai.IAiLLMKeyService;
import com.zboot.framework.service.ai.IStreamChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 流式对话服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamChatServiceImpl implements IStreamChatService {

    private final IAiChatService aiChatService;
    private final IAiLLMKeyService aiLLMKeyService;
    private final LLMFactory llmFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_CACHE_KEY = "chat:session:";
    private static final String KEY_CACHE_KEY = "chat:key:";
    private static final int CACHE_EXPIRE_MINUTES = 30;

    @Override
    public Flux<LLMResponse> streamChat(StreamChatRequest request) {
        return Flux.defer(() -> {
            try {
                // 1. 获取会话信息（优先从缓存获取）
                AiChatSessionDO session = getSessionFromCache(request.getSessionId());
                if (session == null) {
                    log.error("会话不存在: {}", request.getSessionId());
                    return Flux.just(createErrorResponse("会话不存在"));
                }

                // 2. 获取密钥信息（优先从缓存获取）
                AiLLMKeyDO llmKey = getKeyFromCache(session.getKeyId());
                if (llmKey == null) {
                    log.error("密钥不存在: {}", session.getKeyId());
                    return Flux.just(createErrorResponse("密钥不存在"));
                }

                // 3. 获取LLM服务
                LLMService llmService;
                try {
                    llmService = llmFactory.getService(llmKey.getLlmProvider());
                } catch (Exception e) {
                    log.error("不支持的LLM厂商: {}", llmKey.getLlmProvider(), e);
                    return Flux.just(createErrorResponse("不支持的LLM厂商: " + llmKey.getLlmProvider()));
                }

                // 4. 构建LLM请求
                LLMRequest llmRequest = buildLLMRequest(request, session, llmKey);

                // 5. 调用流式接口
                return llmService.chatStream(llmRequest)
                        .timeout(Duration.ofSeconds(60))
                        .doOnNext(response -> {
                            log.debug("接收到流式响应: {}", response);
                        })
                        .doOnError(error -> {
                            log.error("流式对话发生错误: {}", error.getMessage(), error);
                        })
                        .onErrorReturn(createErrorResponse("流式对话调用失败"));

            } catch (Exception e) {
                log.error("流式对话处理异常: {}", e.getMessage(), e);
                return Flux.just(createErrorResponse("系统异常: " + e.getMessage()));
            }
        });
    }

    /**
     * 从缓存获取会话信息
     */
    private AiChatSessionDO getSessionFromCache(Long sessionId) {
        String cacheKey = SESSION_CACHE_KEY + sessionId;
        
        // 先从缓存获取
        AiChatSessionDO session = (AiChatSessionDO) redisTemplate.opsForValue().get(cacheKey);
        if (session != null) {
            log.debug("从缓存获取会话信息: {}", sessionId);
            return session;
        }

        // 缓存未命中，从数据库获取
        session = aiChatService.getById(sessionId);
        if (session != null) {
            // 存入缓存
            redisTemplate.opsForValue().set(cacheKey, session, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("从数据库获取会话信息并缓存: {}", sessionId);
        }
        
        return session;
    }

    /**
     * 从缓存获取密钥信息
     */
    private AiLLMKeyDO getKeyFromCache(Long keyId) {
        String cacheKey = KEY_CACHE_KEY + keyId;
        
        // 先从缓存获取
        AiLLMKeyDO llmKey = (AiLLMKeyDO) redisTemplate.opsForValue().get(cacheKey);
        if (llmKey != null) {
            log.debug("从缓存获取密钥信息: {}", keyId);
            return llmKey;
        }

        // 缓存未命中，从数据库获取
        llmKey = aiLLMKeyService.getById(keyId);
        if (llmKey != null) {
            // 存入缓存
            redisTemplate.opsForValue().set(cacheKey, llmKey, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("从数据库获取密钥信息并缓存: {}", keyId);
        }
        
        return llmKey;
    }

    /**
     * 构建LLM请求
     */
    private LLMRequest buildLLMRequest(StreamChatRequest request, AiChatSessionDO session, AiLLMKeyDO llmKey) {
        return new LLMRequest()
                .setApiKey(llmKey.getApiKey())
                .setSecretKey(llmKey.getSecretKey())
                .setModel(session.getModelName())
                .setUserMessage(request.getContent())
                .setMaxTokens(1024)
                .setTemperature(0.7)
                .setStream(true)
                .setTimeout(60);
    }

    /**
     * 创建错误响应
     */
    private LLMResponse createErrorResponse(String errorMessage) {
        return new LLMResponse()
                .setError(errorMessage)
                .setFinished(true)
                .setProvider("system")
                .setResponseTime(System.currentTimeMillis());
    }
}