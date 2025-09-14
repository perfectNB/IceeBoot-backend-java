/**
 * 大模型配置类
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.service.ai.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 大模型配置类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {
    
    /**
     * 默认厂商
     */
    private String defaultProvider = "deepseek";
    
    /**
     * 默认超时时间（秒）
     */
    private Integer defaultTimeout = 60;
    
    /**
     * 默认最大token数
     */
    private Integer defaultMaxTokens = 4096;
    
    /**
     * 默认温度参数
     */
    private Double defaultTemperature = 0.7;
    
    /**
     * 是否启用流式输出
     */
    private Boolean enableStreaming = true;
    
    /**
     * 是否启用推理模式（针对支持的模型）
     */
    private Boolean enableReasoning = false;
    
    /**
     * SSE心跳间隔（秒）
     */
    private Integer sseHeartbeatInterval = 30;
    
    /**
     * 各厂商配置
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();
    
    /**
     * 厂商配置类
     */
    @Data
    public static class ProviderConfig {
        
        /**
         * 是否启用
         */
        private Boolean enabled = true;
        
        /**
         * API基础URL
         */
        private String baseUrl;
        
        /**
         * 默认模型
         */
        private String defaultModel;
        
        /**
         * 最大token限制
         */
        private Integer maxTokens;
        
        /**
         * 超时时间（秒）
         */
        private Integer timeout;
        
        /**
         * 是否支持流式输出
         */
        private Boolean supportsStreaming = true;
        
        /**
         * 是否支持系统提示词
         */
        private Boolean supportsSystemPrompt = true;
        
        /**
         * 是否支持推理模式
         */
        private Boolean supportsReasoning = false;
        
        /**
         * 扩展配置
         */
        private Map<String, Object> extraConfig = new HashMap<>();
    }
    
    /**
     * 获取厂商配置
     * @param provider 厂商名称
     * @return 厂商配置
     */
    public ProviderConfig getProviderConfig(String provider) {
        return providers.get(provider);
    }
    
    /**
     * 检查厂商是否启用
     * @param provider 厂商名称
     * @return 是否启用
     */
    public boolean isProviderEnabled(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        return config != null && Boolean.TRUE.equals(config.getEnabled());
    }
    
    /**
     * 获取厂商的基础URL
     * @param provider 厂商名称
     * @return 基础URL
     */
    public String getProviderBaseUrl(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        return config != null ? config.getBaseUrl() : null;
    }
    
    /**
     * 获取厂商的默认模型
     * @param provider 厂商名称
     * @return 默认模型
     */
    public String getProviderDefaultModel(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        return config != null ? config.getDefaultModel() : null;
    }
    
    /**
     * 获取厂商的最大token限制
     * @param provider 厂商名称
     * @return 最大token数
     */
    public Integer getProviderMaxTokens(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        if (config != null && config.getMaxTokens() != null) {
            return config.getMaxTokens();
        }
        return defaultMaxTokens;
    }
    
    /**
     * 获取厂商的超时时间
     * @param provider 厂商名称
     * @return 超时时间（秒）
     */
    public Integer getProviderTimeout(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        if (config != null && config.getTimeout() != null) {
            return config.getTimeout();
        }
        return defaultTimeout;
    }
    
    /**
     * 检查厂商是否支持流式输出
     * @param provider 厂商名称
     * @return 是否支持
     */
    public boolean isProviderSupportsStreaming(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        if (config != null && config.getSupportsStreaming() != null) {
            return config.getSupportsStreaming();
        }
        return enableStreaming;
    }
    
    /**
     * 检查厂商是否支持系统提示词
     * @param provider 厂商名称
     * @return 是否支持
     */
    public boolean isProviderSupportsSystemPrompt(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        return config == null || Boolean.TRUE.equals(config.getSupportsSystemPrompt());
    }
    
    /**
     * 检查厂商是否支持推理模式
     * @param provider 厂商名称
     * @return 是否支持
     */
    public boolean isProviderSupportsReasoning(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        return config != null && Boolean.TRUE.equals(config.getSupportsReasoning());
    }
    
    /**
     * 获取厂商的扩展配置
     * @param provider 厂商名称
     * @param key 配置键
     * @return 配置值
     */
    public Object getProviderExtraConfig(String provider, String key) {
        ProviderConfig config = getProviderConfig(provider);
        if (config != null && config.getExtraConfig() != null) {
            return config.getExtraConfig().get(key);
        }
        return null;
    }
}