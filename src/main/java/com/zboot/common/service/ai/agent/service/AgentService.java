/**
 * Agent服务抽象接口
 * 定义Agent的通用操作方法，使用策略模式
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.zboot.common.service.ai.agent.service;

import com.zboot.common.service.ai.agent.dto.AgentMessage;
import com.zboot.common.service.ai.agent.dto.AgentRequest;
import com.zboot.common.service.ai.agent.dto.AgentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Agent服务抽象接口
 * @author CodeIcee
 * @date 2025-09-04
 */
public interface AgentService {
    
    /**
     * 获取厂商名称
     * @return 厂商名称
     */
    String getProvider();
    
    /**
     * 智能体对话（非流式）
     * @param request 请求参数
     * @return 响应结果
     */
    Mono<AgentResponse> chat(AgentRequest request);
    
    /**
     * 智能体对话（流式）
     * @param request 请求参数
     * @return 流式响应结果
     */
    Flux<AgentResponse> chatStream(AgentRequest request);
    
    /**
     * 执行工作流（非流式）
     * @param request 请求参数
     * @return 响应结果
     */
    Mono<AgentResponse> runWorkflow(AgentRequest request);
    
    /**
     * 执行工作流（流式）
     * @param request 请求参数
     * @return 流式响应结果
     */
    Flux<AgentResponse> runWorkflowStream(AgentRequest request);
    
    /**
     * 创建会话
     * @param userId 用户ID
     * @param agentId 智能体ID
     * @param apiKey API密钥
     * @return 会话ID
     */
    Mono<String> createConversation(String userId, String agentId, String apiKey);
    
    /**
     * 获取会话历史
     * @param conversationId 会话ID
     * @param apiKey API密钥
     * @return 消息列表
     */
    Mono<List<AgentMessage>> getConversationHistory(String conversationId, String apiKey);
    
    /**
     * 删除会话
     * @param conversationId 会话ID
     * @param apiKey API密钥
     * @return 是否成功
     */
    Mono<Boolean> deleteConversation(String conversationId, String apiKey);
    
    /**
     * 验证API密钥是否有效
     * @param apiKey API密钥
     * @param baseUrl 基础URL（可选）
     * @return 是否有效
     */
    Mono<Boolean> validateApiKey(String apiKey, String baseUrl);
    
    /**
     * 获取支持的智能体列表
     * @param apiKey API密钥
     * @param baseUrl 基础URL（可选）
     * @return 智能体列表
     */
    Mono<List<String>> getSupportedAgents(String apiKey, String baseUrl);
    
    /**
     * 获取支持的工作流列表
     * @param apiKey API密钥
     * @param baseUrl 基础URL（可选）
     * @return 工作流列表
     */
    Mono<List<String>> getSupportedWorkflows(String apiKey, String baseUrl);
    
    /**
     * 检查服务是否可用
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * 是否支持流式输出
     * @return 是否支持
     */
    default boolean supportsStreaming() {
        return true;
    }
    
    /**
     * 是否支持工作流
     * @return 是否支持
     */
    default boolean supportsWorkflow() {
        return true;
    }
    
    /**
     * 是否支持智能体
     * @return 是否支持
     */
    default boolean supportsAgent() {
        return true;
    }
    
    /**
     * 是否支持工具调用
     * @return 是否支持
     */
    default boolean supportsToolCalls() {
        return true;
    }
    
    /**
     * 获取默认超时时间（秒）
     * @return 超时时间
     */
    default Integer getDefaultTimeout() {
        return 60;
    }
    
    /**
     * 获取默认基础URL
     * @return 基础URL
     */
    String getDefaultBaseUrl();
}