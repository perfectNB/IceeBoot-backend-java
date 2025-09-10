/**
 * Agent配置类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.zboot.common.service.ai.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent配置类
 * 用于配置各个厂商的Agent服务参数
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {
    
    /**
     * 默认厂商
     */
    private String defaultProvider = "coze";
    
    /**
     * 是否启用Agent服务
     */
    private Boolean enabled = true;
    
    /**
     * 全局配置
     */
    private GlobalConfig global = new GlobalConfig();
    
    /**
     * Coze配置
     */
    private CozeConfig coze = new CozeConfig();
    
    /**
     * Dify配置
     */
    private DifyConfig dify = new DifyConfig();
    
    /**
     * 全局配置类
     */
    @Data
    public static class GlobalConfig {
        /**
         * 默认超时时间（秒）
         */
        private Integer timeout = 60;
        
        /**
         * 最大重试次数
         */
        private Integer maxRetries = 3;
        
        /**
         * 是否启用流式输出
         */
        private Boolean enableStreaming = true;
        
        /**
         * 是否启用日志记录
         */
        private Boolean enableLogging = true;
        
        /**
         * 连接池大小
         */
        private Integer connectionPoolSize = 10;
    }
    
    /**
     * Coze配置类
     */
    @Data
    public static class CozeConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = true;
        
        /**
         * API Token
         */
        private String apiToken;
        
        /**
         * 基础URL
         */
        private String baseUrl = "https://api.coze.com";
        
        /**
         * 中国区URL
         */
        private String cnBaseUrl = "https://api.coze.cn";
        
        /**
         * 默认智能体ID
         */
        private String defaultAgentId;
        
        /**
         * 默认工作流ID
         */
        private String defaultWorkflowId;
        
        /**
         * 超时时间（秒）
         */
        private Integer timeout = 60;
        
        /**
         * 是否使用中国区
         */
        private Boolean useCnRegion = false;
        
        /**
         * 额外参数
         */
        private Map<String, Object> extraParams = new HashMap<>();
    }
    
    /**
     * Dify配置类
     */
    @Data
    public static class DifyConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = true;
        
        /**
         * API Key
         */
        private String apiKey;
        
        /**
         * 基础URL
         */
        private String baseUrl = "https://api.dify.ai";
        
        /**
         * 默认应用ID
         */
        private String defaultAppId;
        
        /**
         * 默认工作流ID
         */
        private String defaultWorkflowId;
        
        /**
         * 超时时间（秒）
         */
        private Integer timeout = 60;
        
        /**
         * 是否自动保存会话
         */
        private Boolean autoSave = true;
        
        /**
         * 额外参数
         */
        private Map<String, Object> extraParams = new HashMap<>();
    }
    
    /**
     * 获取指定厂商的配置
     * @param provider 厂商名称
     * @return 厂商配置
     */
    public Object getProviderConfig(String provider) {
        if (provider == null) {
            return null;
        }
        
        switch (provider.toLowerCase()) {
            case "coze":
                return coze;
            case "dify":
                return dify;
            default:
                return null;
        }
    }
    
    /**
     * 检查厂商是否启用
     * @param provider 厂商名称
     * @return 是否启用
     */
    public boolean isProviderEnabled(String provider) {
        if (!enabled) {
            return false;
        }
        
        Object config = getProviderConfig(provider);
        if (config instanceof CozeConfig) {
            return ((CozeConfig) config).getEnabled();
        } else if (config instanceof DifyConfig) {
            return ((DifyConfig) config).getEnabled();
        }
        
        return false;
    }
    
    /**
     * 获取厂商的基础URL
     * @param provider 厂商名称
     * @return 基础URL
     */
    public String getProviderBaseUrl(String provider) {
        Object config = getProviderConfig(provider);
        if (config instanceof CozeConfig) {
            CozeConfig cozeConfig = (CozeConfig) config;
            return cozeConfig.getUseCnRegion() ? cozeConfig.getCnBaseUrl() : cozeConfig.getBaseUrl();
        } else if (config instanceof DifyConfig) {
            return ((DifyConfig) config).getBaseUrl();
        }
        
        return null;
    }
    
    /**
     * 获取厂商的超时时间
     * @param provider 厂商名称
     * @return 超时时间
     */
    public Integer getProviderTimeout(String provider) {
        Object config = getProviderConfig(provider);
        if (config instanceof CozeConfig) {
            return ((CozeConfig) config).getTimeout();
        } else if (config instanceof DifyConfig) {
            return ((DifyConfig) config).getTimeout();
        }
        
        return global.getTimeout();
    }
    
    /**
     * 获取厂商的API密钥
     * @param provider 厂商名称
     * @return API密钥
     */
    public String getProviderApiKey(String provider) {
        Object config = getProviderConfig(provider);
        if (config instanceof CozeConfig) {
            return ((CozeConfig) config).getApiToken();
        } else if (config instanceof DifyConfig) {
            return ((DifyConfig) config).getApiKey();
        }
        
        return null;
    }
    
    /**
     * 获取厂商的默认智能体ID
     * @param provider 厂商名称
     * @return 默认智能体ID
     */
    public String getProviderDefaultAgentId(String provider) {
        Object config = getProviderConfig(provider);
        if (config instanceof CozeConfig) {
            return ((CozeConfig) config).getDefaultAgentId();
        } else if (config instanceof DifyConfig) {
            return ((DifyConfig) config).getDefaultAppId();
        }
        
        return null;
    }
    
    /**
     * 获取厂商的默认工作流ID
     * @param provider 厂商名称
     * @return 默认工作流ID
     */
    public String getProviderDefaultWorkflowId(String provider) {
        Object config = getProviderConfig(provider);
        if (config instanceof CozeConfig) {
            return ((CozeConfig) config).getDefaultWorkflowId();
        } else if (config instanceof DifyConfig) {
            return ((DifyConfig) config).getDefaultWorkflowId();
        }
        
        return null;
    }
    
    /**
     * 获取所有启用的厂商
     * @return 启用的厂商列表
     */
    public java.util.List<String> getEnabledProviders() {
        java.util.List<String> enabledProviders = new java.util.ArrayList<>();
        
        if (isProviderEnabled("coze")) {
            enabledProviders.add("coze");
        }
        if (isProviderEnabled("dify")) {
            enabledProviders.add("dify");
        }
        
        return enabledProviders;
    }
    
    /**
     * 生成配置摘要
     * @return 配置摘要
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Agent配置摘要:\n");
        summary.append("- 服务启用: ").append(enabled).append("\n");
        summary.append("- 默认厂商: ").append(defaultProvider).append("\n");
        summary.append("- 全局超时: ").append(global.getTimeout()).append("秒\n");
        summary.append("- 启用的厂商: ").append(getEnabledProviders()).append("\n");
        
        return summary.toString();
    }
}