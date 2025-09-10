/**
 * 大模型服务抽象接口
 * 定义大模型的通用操作方法，使用策略模式
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.ai.llm.service;

import com.zboot.common.service.ai.llm.dto.LLMRequest;
import com.zboot.common.service.ai.llm.dto.LLMResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 大模型服务抽象接口
 * @author CodeIcee
 * @date 2025-09-03
 */
public interface LLMService {
    
    /**
     * 获取厂商名称
     * @return 厂商名称
     */
    String getProvider();
    
    /**
     * 非流式聊天
     * @param request 请求参数
     * @return 响应结果
     */
    Mono<LLMResponse> chat(LLMRequest request);
    
    /**
     * 流式聊天
     * @param request 请求参数
     * @return 流式响应结果
     */
    Flux<LLMResponse> chatStream(LLMRequest request);
    
    /**
     * 验证API密钥是否有效
     * @param apiKey API密钥
     * @param baseUrl 基础URL（可选）
     * @return 是否有效
     */
    Mono<Boolean> validateApiKey(String apiKey, String baseUrl);
    
    /**
     * 获取支持的模型列表
     * @param apiKey API密钥
     * @param baseUrl 基础URL（可选）
     * @return 模型列表
     */
    Mono<java.util.List<String>> getSupportedModels(String apiKey, String baseUrl);
    
    /**
     * 检查服务是否可用
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * 获取默认模型名称
     * @return 默认模型名称
     */
    String getDefaultModel();
    
    /**
     * 获取最大token限制
     * @param model 模型名称
     * @return 最大token数
     */
    default Integer getMaxTokens(String model) {
        return 4096;
    }
    
    /**
     * 是否支持流式输出
     * @return 是否支持
     */
    default boolean supportsStreaming() {
        return true;
    }
    
    /**
     * 是否支持系统提示词
     * @return 是否支持
     */
    default boolean supportsSystemPrompt() {
        return true;
    }
}