/**
 * 大模型请求DTO
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.llm.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 大模型请求DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class LLMRequest {
    
    /**
     * 消息列表
     */
    private List<LLMMessage> messages;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 温度参数，控制输出的随机性 (0.0-2.0)
     */
    private Double temperature;
    
    /**
     * 最大输出token数
     */
    private Integer maxTokens;
    
    /**
     * 是否流式输出
     */
    private Boolean stream;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 用户输入内容
     */
    private String userMessage;
    
    /**
     * 扩展参数，用于不同厂商的特殊参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 密钥（用于文心一言等需要双密钥认证的服务）
     */
    private String secretKey;
    
    /**
     * 基础URL
     */
    private String baseUrl;
    
    /**
     * 请求超时时间（秒）
     */
    private Integer timeout;
}