package com.iceeboot.common.service.oss.factory;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.oss.service.AbstractOSSService;
import com.iceeboot.common.service.oss.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OSS工厂类
 * 通过Spring自动注入所有OSSService实现，根据厂商名称自动路由
 * 新增厂商只需实现OSSService接口并添加@Component即可，无需修改此类
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class OSSFactory {

    private final Map<String, OSSService> serviceMap;

    public OSSFactory(List<OSSService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                        s -> s.getProvider().toLowerCase(),
                        Function.identity(),
                        (a, b) -> a
                ));
        log.info("OSSFactory初始化完成，注册{}个厂商: {}", serviceMap.size(), serviceMap.keySet());
    }

    /**
     * 根据厂商名称获取OSS服务实例
     */
    public OSSService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IceeBootException("OSS厂商名称不能为空");
        }
        OSSService service = serviceMap.get(provider.toLowerCase().trim());
        if (service == null) {
            throw new IceeBootException("不支持的OSS厂商: " + provider);
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
        OSSService service = serviceMap.get(provider != null ? provider.toLowerCase().trim() : "");
        if (service instanceof AbstractOSSService) {
            return ((AbstractOSSService<?>) service).getProviderDisplayName();
        }
        return provider != null ? provider : "未知";
    }

    /**
     * 获取厂商的默认端点
     */
    public String getDefaultEndpoint(String provider) {
        OSSService service = serviceMap.get(provider != null ? provider.toLowerCase().trim() : "");
        if (service instanceof AbstractOSSService) {
            return ((AbstractOSSService<?>) service).getDefaultEndpoint();
        }
        return null;
    }
}
