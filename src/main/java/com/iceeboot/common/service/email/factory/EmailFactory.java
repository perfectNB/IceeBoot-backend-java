package com.iceeboot.common.service.email.factory;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.email.service.AbstractEmailService;
import com.iceeboot.common.service.email.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Email工厂类
 * 通过Spring自动注入所有EmailService实现，根据厂商名称自动路由
 * 新增厂商只需实现EmailService接口并添加@Component即可，无需修改此类
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
@Component
public class EmailFactory {

    private final Map<String, EmailService> serviceMap;

    public EmailFactory(List<EmailService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                        s -> s.getProvider().toLowerCase(),
                        Function.identity(),
                        (a, b) -> a
                ));
        log.info("EmailFactory初始化完成，注册{}个厂商: {}", serviceMap.size(), serviceMap.keySet());
    }

    /**
     * 根据厂商名称获取Email服务实例
     */
    public EmailService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IceeBootException("Email厂商名称不能为空");
        }
        EmailService service = serviceMap.get(provider.toLowerCase().trim());
        if (service == null) {
            throw new IceeBootException("不支持的Email厂商: " + provider);
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
        EmailService service = serviceMap.get(provider != null ? provider.toLowerCase().trim() : "");
        if (service instanceof AbstractEmailService) {
            return ((AbstractEmailService) service).getProviderDisplayName();
        }
        return provider != null ? provider : "未知";
    }

    /**
     * 获取厂商的默认SMTP服务器
     */
    public String getDefaultSmtpHost(String provider) {
        EmailService service = serviceMap.get(provider != null ? provider.toLowerCase().trim() : "");
        if (service instanceof AbstractEmailService) {
            return ((AbstractEmailService) service).getDefaultSmtpHost();
        }
        return null;
    }

    /**
     * 获取厂商的默认SMTP端口
     */
    public int getDefaultSmtpPort(String provider, boolean useSsl) {
        EmailService service = serviceMap.get(provider != null ? provider.toLowerCase().trim() : "");
        if (service instanceof AbstractEmailService) {
            AbstractEmailService abs = (AbstractEmailService) service;
            return useSsl ? abs.getDefaultSslPort() : abs.getDefaultSmtpPort();
        }
        return useSsl ? 465 : 587;
    }

    /**
     * 根据邮箱地址自动识别厂商
     */
    public String detectProvider(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        String domain = email.toLowerCase().split("@")[1];
        for (Map.Entry<String, EmailService> entry : serviceMap.entrySet()) {
            // 简单匹配：厂商名称包含在域名中
            if (domain.contains(entry.getKey())) {
                return entry.getKey();
            }
        }
        // 常见邮箱域名映射
        if (domain.equals("qq.com")) return "qq";
        if (domain.equals("163.com") || domain.equals("126.com") || domain.equals("yeah.net")) return "netease";
        if (domain.equals("gmail.com")) return "gmail";
        if (domain.equals("outlook.com") || domain.equals("hotmail.com") || domain.equals("live.com")) return "outlook";
        return null;
    }
}
