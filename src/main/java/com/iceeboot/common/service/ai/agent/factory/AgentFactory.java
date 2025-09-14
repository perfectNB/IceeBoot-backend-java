/**
 * Agent工厂类
 * 根据厂商名称获取对应的Agent服务实例
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.ai.agent.factory;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.ai.agent.service.AgentService;
import com.iceeboot.common.service.ai.agent.service.impl.CozeAgentService;
import com.iceeboot.common.service.ai.agent.service.impl.DifyAgentService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Agent工厂类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Component
public class AgentFactory {
    
    /**
     * 服务实例缓存
     */
    private final Map<String, AgentService> serviceMap = new HashMap<>();
    
    /**
     * 构造函数，初始化所有服务实例
     */
    public AgentFactory() {
        initServices();
    }
    
    /**
     * 初始化所有服务实例
     */
    private void initServices() {
        // Coze
        serviceMap.put("coze", new CozeAgentService());
        
        // Dify
        serviceMap.put("dify", new DifyAgentService());
    }
    
    /**
     * 根据厂商名称获取Agent服务实例
     * @param provider 厂商名称
     * @return Agent服务实例
     */
    public AgentService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IceeBootException("厂商名称不能为空");
        }
        
        String normalizedProvider = provider.toLowerCase().trim();
        AgentService service = serviceMap.get(normalizedProvider);
        
        if (service == null) {
            throw new IceeBootException("不支持的Agent厂商: " + provider);
        }
        
        return service;
    }
    
    /**
     * 获取所有支持的厂商名称
     * @return 厂商名称集合
     */
    public Set<String> getSupportedProviders() {
        return serviceMap.keySet();
    }
    
    /**
     * 检查是否支持指定厂商
     * @param provider 厂商名称
     * @return 是否支持
     */
    public boolean isSupported(String provider) {
        return provider != null && serviceMap.containsKey(provider.toLowerCase().trim());
    }
    
    /**
     * 获取默认厂商服务（Coze）
     * @return 默认服务实例
     */
    public AgentService getDefaultService() {
        return getService("coze");
    }
    
    /**
     * 获取服务实例数量
     * @return 实例数量
     */
    public int getServiceCount() {
        return serviceMap.size();
    }
    
    /**
     * 清空服务缓存
     */
    public void clearCache() {
        serviceMap.clear();
        initServices();
    }
    
    /**
     * 获取厂商显示名称
     * @param provider 厂商名称
     * @return 显示名称
     */
    public String getProviderDisplayName(String provider) {
        if (provider == null) {
            return "未知";
        }
        
        switch (provider.toLowerCase()) {
            case "coze":
                return "Coze";
            case "dify":
                return "Dify";
            default:
                return provider;
        }
    }
    
    /**
     * 获取厂商默认端点
     * @param provider 厂商名称
     * @return 默认端点
     */
    public String getProviderDefaultEndpoint(String provider) {
        AgentService service = serviceMap.get(provider.toLowerCase());
        return service != null ? service.getDefaultBaseUrl() : null;
    }
    
    /**
     * 获取厂商支持的功能
     * @param provider 厂商名称
     * @return 功能描述
     */
    public Map<String, Boolean> getProviderFeatures(String provider) {
        AgentService service = serviceMap.get(provider.toLowerCase());
        if (service == null) {
            return new HashMap<>();
        }
        
        Map<String, Boolean> features = new HashMap<>();
        features.put("streaming", service.supportsStreaming());
        features.put("workflow", service.supportsWorkflow());
        features.put("agent", service.supportsAgent());
        features.put("toolCalls", service.supportsToolCalls());
        
        return features;
    }
    
    /**
     * 获取厂商认证信息
     * @param provider 厂商名称
     * @return 认证信息描述
     */
    public String getProviderAuthInfo(String provider) {
        switch (provider.toLowerCase()) {
            case "coze":
                return "需要Coze API Token，可在Coze开放平台获取";
            case "dify":
                return "需要Dify API Key，可在Dify应用设置中获取";
            default:
                return "请查看对应厂商文档获取认证信息";
        }
    }
}