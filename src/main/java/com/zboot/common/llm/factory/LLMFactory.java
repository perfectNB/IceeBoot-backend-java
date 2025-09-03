/**
 * 大模型工厂类
 * 根据厂商名称获取对应的大模型服务实例
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.llm.factory;

import com.zboot.common.constant.SystemConstants;
import com.zboot.common.llm.service.LLMService;
import com.zboot.common.llm.service.impl.DeepSeekService;
import com.zboot.common.llm.service.impl.ChatGPTService;
import com.zboot.common.llm.service.impl.ClaudeService;
import com.zboot.common.llm.service.impl.GeminiService;
import com.zboot.common.llm.service.impl.WenxinService;
import com.zboot.common.llm.service.impl.QwenService;
import com.zboot.common.llm.service.impl.MoonshotService;
import com.zboot.common.llm.service.impl.GrokService;
import com.zboot.common.llm.service.impl.ZhipuService;
import com.zboot.common.llm.service.impl.BaichuanService;
import com.zboot.common.llm.service.impl.SparkService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 大模型工厂类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Component
public class LLMFactory {
    
    /**
     * 服务实例缓存
     */
    private final Map<String, LLMService> serviceMap = new HashMap<>();
    
    /**
     * 构造函数，初始化所有服务实例
     */
    public LLMFactory() {
        initServices();
    }
    
    /**
     * 初始化所有服务实例
     */
    private void initServices() {
        // DeepSeek
        serviceMap.put(SystemConstants.LLMProvider.DEEPSEEK, new DeepSeekService());
        
        // ChatGPT
        serviceMap.put(SystemConstants.LLMProvider.CHATGPT, new ChatGPTService());
        
        // Claude
        serviceMap.put(SystemConstants.LLMProvider.CLAUDE, new ClaudeService());
        
        // Gemini
        serviceMap.put(SystemConstants.LLMProvider.GEMINI, new GeminiService());
        
        // 文心一言
        serviceMap.put(SystemConstants.LLMProvider.WENXIN, new WenxinService());
        
        // 通义千问
        serviceMap.put(SystemConstants.LLMProvider.QWEN, new QwenService());
        
        // 月之暗面 (Kimi)
        serviceMap.put(SystemConstants.LLMProvider.MOONSHOT, new MoonshotService());
        
        // Grok
        serviceMap.put(SystemConstants.LLMProvider.GROK, new GrokService());
        
        // 智谱AI
        serviceMap.put(SystemConstants.LLMProvider.ZHIPU, new ZhipuService());
        
        // 百川智能
        serviceMap.put(SystemConstants.LLMProvider.BAICHUAN, new BaichuanService());
        
        // 讯飞星火
        serviceMap.put(SystemConstants.LLMProvider.SPARK, new SparkService());
    }
    
    /**
     * 根据厂商名称获取服务实例
     * @param provider 厂商名称
     * @return 服务实例
     * @throws IllegalArgumentException 如果厂商不支持
     */
    public LLMService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("厂商名称不能为空");
        }
        
        LLMService service = serviceMap.get(provider.toLowerCase());
        if (service == null) {
            throw new IllegalArgumentException("不支持的厂商: " + provider);
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
        return provider != null && serviceMap.containsKey(provider.toLowerCase());
    }
    
    /**
     * 获取默认厂商服务（DeepSeek）
     * @return 默认服务实例
     */
    public LLMService getDefaultService() {
        return getService(SystemConstants.LLMProvider.DEEPSEEK);
    }
}