/**
 * SMS工厂类
 *
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.iceeboot.common.service.sms.factory;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.sms.service.AbstractSMSService;
import com.iceeboot.common.service.sms.service.SMSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SMS工厂类
 * 通过Spring自动注入所有SMSService实现，根据厂商名称自动路由
 * 新增厂商只需实现SMSService接口并添加@Component即可，无需修改此类
 *
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
@Component
public class SMSFactory {

    private final Map<String, SMSService> serviceMap;

    public SMSFactory(List<SMSService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                        s -> s.getProvider().toLowerCase(),
                        Function.identity(),
                        (a, b) -> a
                ));
        log.info("SMSFactory初始化完成，注册{}个厂商: {}", serviceMap.size(), serviceMap.keySet());
    }

    /**
     * 根据厂商名称获取SMS服务实例
     */
    public SMSService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IceeBootException(ResponseMessage.Factory.SMS_PROVIDER_EMPTY);
        }
        SMSService service = serviceMap.get(provider.toLowerCase().trim());
        if (service == null) {
            throw new IceeBootException(ResponseMessage.Factory.SMS_PROVIDER_NOT_SUPPORTED + provider);
        }
        return service;
    }

    /**
     * 获取所有支持的厂商列表
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
     * 获取厂商的友好名称
     */
    public String getProviderDisplayName(String provider) {
        SMSService service = serviceMap.get(provider != null ? provider.toLowerCase().trim() : "");
        if (service instanceof AbstractSMSService) {
            return ((AbstractSMSService) service).getProviderDisplayName();
        }
        return provider != null ? provider : ResponseMessage.Factory.UNKNOWN_PROVIDER;
    }

    /**
     * 获取厂商的默认端点
     */
    public String getDefaultEndpoint(String provider) {
        SMSService service = serviceMap.get(provider != null ? provider.toLowerCase().trim() : "");
        if (service instanceof AbstractSMSService) {
            return ((AbstractSMSService) service).getDefaultEndpoint();
        }
        return null;
    }
}
