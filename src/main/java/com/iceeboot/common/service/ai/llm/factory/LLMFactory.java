/**
 * 大模型工厂类
 * 根据厂商名称获取对应的大模型服务实例
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.service.ai.llm.factory;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 大模型工厂类
 * 通过Spring自动注入所有LLMService实现，根据厂商名称自动路由
 * 新增厂商只需实现LLMService接口并添加@Component即可，无需修改此类
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class LLMFactory {

    private final Map<String, LLMService> serviceMap;

    public LLMFactory(List<LLMService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                        s -> s.getProvider().toLowerCase(),
                        Function.identity(),
                        (a, b) -> a
                ));
        log.info("LLMFactory初始化完成，注册{}个厂商: {}", serviceMap.size(), serviceMap.keySet());
    }

    /**
     * 根据厂商名称获取服务实例
     */
    public LLMService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IceeBootException(ResponseMessage.Factory.LLM_PROVIDER_EMPTY);
        }
        LLMService service = serviceMap.get(provider.toLowerCase().trim());
        if (service == null) {
            throw new IceeBootException(ResponseMessage.Factory.LLM_PROVIDER_NOT_SUPPORTED + provider);
        }
        return service;
    }

    /**
     * 获取所有支持的厂商名称
     */
    public Set<String> getSupportedProviders() {
        return serviceMap.keySet();
    }

    /**
     * 检查是否支持指定厂商
     */
    public boolean isSupported(String provider) {
        return provider != null && serviceMap.containsKey(provider.toLowerCase().trim());
    }

    /**
     * 获取默认厂商服务（DeepSeek）
     */
    public LLMService getDefaultService() {
        return getService(SystemConstants.LLMProvider.DEEPSEEK);
    }
}
