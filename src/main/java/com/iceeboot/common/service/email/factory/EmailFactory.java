package com.iceeboot.common.service.email.factory;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.email.service.EmailService;
import com.iceeboot.common.service.email.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Email工厂类
 * 用于根据厂商名称获取对应的Email服务实例
 * 采用策略模式 + 工厂模式实现多厂商Email服务的统一管理
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
@Component
public class EmailFactory {
    
    /**
     * Email服务实例缓存
     * 使用ConcurrentHashMap保证线程安全
     */
    private static final Map<String, EmailService> EMAIL_SERVICE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 支持的Email厂商常量
     */
    public static final class Providers {
        public static final String QQ = "qq";
        public static final String NETEASE = "netease";
        public static final String GMAIL = "gmail";
        public static final String OUTLOOK = "outlook";
        public static final String ALIYUN = "aliyun";
        public static final String TENCENT = "tencent";
    }
    
    /**
     * 根据厂商名称获取Email服务实例
     * 
     * @param provider 厂商名称
     * @return Email服务实例
     * @throws IceeBootException 当厂商名称不支持时抛出异常
     */
    public static EmailService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IceeBootException("Email厂商名称不能为空");
        }
        
        String normalizedProvider = provider.toLowerCase().trim();
        
        // 从缓存中获取服务实例
        EmailService service = EMAIL_SERVICE_CACHE.get(normalizedProvider);
        if (service != null) {
            return service;
        }
        
        // 创建新的服务实例
        service = createService(normalizedProvider);
        if (service != null) {
            // 缓存服务实例
            EMAIL_SERVICE_CACHE.put(normalizedProvider, service);
            log.info("创建Email服务实例: {}", normalizedProvider);
        }
        
        return service;
    }
    
    /**
     * 根据厂商名称创建Email服务实例
     * 
     * @param provider 厂商名称
     * @return Email服务实例
     */
    private static EmailService createService(String provider) {
        switch (provider) {
            case Providers.QQ:
                return new QQEmailService();
            case Providers.NETEASE:
                return new NeteaseEmailService();
            case Providers.GMAIL:
                return new GmailEmailService();
            case Providers.OUTLOOK:
                return new OutlookEmailService();
            case Providers.ALIYUN:
                return new AliyunEmailService();
            case Providers.TENCENT:
                return new TencentEmailService();
            default:
                log.error("不支持的Email厂商: {}", provider);
                throw new IceeBootException("不支持的Email厂商: " + provider);
        }
    }
    
    /**
     * 获取所有支持的厂商列表
     * 
     * @return 支持的厂商列表
     */
    public static String[] getSupportedProviders() {
        return new String[]{
                Providers.QQ,
                Providers.NETEASE,
                Providers.GMAIL,
                Providers.OUTLOOK,
                Providers.ALIYUN,
                Providers.TENCENT
        };
    }
    
    /**
     * 检查是否支持指定厂商
     * 
     * @param provider 厂商名称
     * @return 是否支持
     */
    public static boolean isSupported(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return false;
        }
        
        String normalizedProvider = provider.toLowerCase().trim();
        for (String supportedProvider : getSupportedProviders()) {
            if (supportedProvider.equals(normalizedProvider)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 清除指定厂商的服务实例缓存
     * 
     * @param provider 厂商名称
     */
    public static void clearCache(String provider) {
        if (provider != null) {
            String normalizedProvider = provider.toLowerCase().trim();
            EMAIL_SERVICE_CACHE.remove(normalizedProvider);
            log.info("清除Email服务实例缓存: {}", normalizedProvider);
        }
    }
    
    /**
     * 清除所有服务实例缓存
     */
    public static void clearAllCache() {
        EMAIL_SERVICE_CACHE.clear();
        log.info("清除所有Email服务实例缓存");
    }
    
    /**
     * 获取当前缓存的服务实例数量
     * 
     * @return 缓存的服务实例数量
     */
    public static int getCacheSize() {
        return EMAIL_SERVICE_CACHE.size();
    }
    
    /**
     * 获取厂商的友好名称
     * 
     * @param provider 厂商名称
     * @return 友好名称
     */
    public static String getProviderDisplayName(String provider) {
        if (provider == null) {
            return "未知";
        }
        
        switch (provider.toLowerCase().trim()) {
            case Providers.QQ:
                return "QQ邮箱";
            case Providers.NETEASE:
                return "网易邮箱";
            case Providers.GMAIL:
                return "Gmail";
            case Providers.OUTLOOK:
                return "Outlook邮箱";
            case Providers.ALIYUN:
                return "阿里云邮箱";
            case Providers.TENCENT:
                return "腾讯企业邮箱";
            default:
                return provider;
        }
    }
    
    /**
     * 获取厂商的默认SMTP服务器
     * 
     * @param provider 厂商名称
     * @return 默认SMTP服务器
     */
    public static String getDefaultSmtpHost(String provider) {
        if (provider == null) {
            return null;
        }
        
        switch (provider.toLowerCase().trim()) {
            case Providers.QQ:
                return "smtp.qq.com";
            case Providers.NETEASE:
                return "smtp.163.com";
            case Providers.GMAIL:
                return "smtp.gmail.com";
            case Providers.OUTLOOK:
                return "smtp-mail.outlook.com";
            case Providers.ALIYUN:
                return "smtp.mxhichina.com";
            case Providers.TENCENT:
                return "smtp.exmail.qq.com";
            default:
                return null;
        }
    }
    
    /**
     * 获取厂商的默认SMTP端口
     * 
     * @param provider 厂商名称
     * @param useSsl 是否使用SSL
     * @return 默认SMTP端口
     */
    public static Integer getDefaultSmtpPort(String provider, boolean useSsl) {
        if (provider == null) {
            return useSsl ? 465 : 587;
        }
        
        switch (provider.toLowerCase().trim()) {
            case Providers.QQ:
                return useSsl ? 465 : 587;
            case Providers.NETEASE:
                return useSsl ? 465 : 25;
            case Providers.GMAIL:
                return useSsl ? 465 : 587;
            case Providers.OUTLOOK:
                return useSsl ? 465 : 587;
            case Providers.ALIYUN:
                return useSsl ? 465 : 25;
            case Providers.TENCENT:
                return useSsl ? 465 : 25;
            default:
                return useSsl ? 465 : 587;
        }
    }
    
    /**
     * 获取厂商支持的邮件类型
     * 
     * @param provider 厂商名称
     * @return 支持的邮件类型列表
     */
    public static String[] getSupportedEmailTypes(String provider) {
        // 所有厂商都支持的基本邮件类型
        return new String[]{"text", "html", "template", "notification", "marketing", "system"};
    }
    
    /**
     * 获取厂商支持的功能列表
     * 
     * @param provider 厂商名称
     * @return 支持的功能列表
     */
    public static String[] getSupportedFeatures(String provider) {
        if (provider == null) {
            return new String[0];
        }
        
        // 所有厂商都支持的基本功能
        String[] basicFeatures = {
                "single", "batch", "template", "attachment", "html", "text", 
                "cc", "bcc", "priority", "reply_to", "custom_headers"
        };
        
        switch (provider.toLowerCase().trim()) {
            case Providers.QQ:
            case Providers.NETEASE:
            case Providers.GMAIL:
            case Providers.OUTLOOK:
            case Providers.ALIYUN:
            case Providers.TENCENT:
                return basicFeatures;
            default:
                return new String[]{"single", "text"};
        }
    }
    
    /**
     * 检查厂商是否支持指定功能
     * 
     * @param provider 厂商名称
     * @param feature 功能名称
     * @return 是否支持
     */
    public static boolean supportsFeature(String provider, String feature) {
        if (provider == null || feature == null) {
            return false;
        }
        
        String[] supportedFeatures = getSupportedFeatures(provider);
        for (String supportedFeature : supportedFeatures) {
            if (supportedFeature.equals(feature)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取厂商的认证方式说明
     * 
     * @param provider 厂商名称
     * @return 认证方式说明
     */
    public static String getAuthenticationInfo(String provider) {
        if (provider == null) {
            return "未知认证方式";
        }
        
        switch (provider.toLowerCase().trim()) {
            case Providers.QQ:
                return "需要QQ邮箱地址和授权码（非QQ密码）";
            case Providers.NETEASE:
                return "需要网易邮箱地址和授权码或客户端专用密码";
            case Providers.GMAIL:
                return "需要Gmail地址和应用专用密码（开启两步验证后生成）";
            case Providers.OUTLOOK:
                return "需要Outlook邮箱地址和密码或应用密码";
            case Providers.ALIYUN:
                return "需要阿里云邮箱地址和密码";
            case Providers.TENCENT:
                return "需要腾讯企业邮箱地址和密码";
            default:
                return "请查看对应厂商文档";
        }
    }
    
    /**
     * 获取厂商的配置建议
     * 
     * @param provider 厂商名称
     * @return 配置建议
     */
    public static String getConfigurationTips(String provider) {
        if (provider == null) {
            return "请配置正确的SMTP服务器信息";
        }
        
        switch (provider.toLowerCase().trim()) {
            case Providers.QQ:
                return "建议使用SSL连接（端口465），需要在QQ邮箱设置中开启SMTP服务并获取授权码";
            case Providers.NETEASE:
                return "支持SSL和TLS连接，需要在邮箱设置中开启SMTP服务";
            case Providers.GMAIL:
                return "必须使用SSL/TLS连接，需要开启两步验证并生成应用专用密码";
            case Providers.OUTLOOK:
                return "建议使用TLS连接（端口587），支持OAuth2.0和基本认证";
            case Providers.ALIYUN:
                return "企业邮箱需要使用正确的SMTP服务器地址，支持SSL和TLS";
            case Providers.TENCENT:
                return "腾讯企业邮箱使用smtp.exmail.qq.com，支持SSL和TLS连接";
            default:
                return "请根据邮箱服务商文档配置SMTP参数";
        }
    }
    
    /**
     * 根据邮箱地址自动识别厂商
     * 
     * @param email 邮箱地址
     * @return 厂商名称，如果无法识别返回null
     */
    public static String detectProvider(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        
        String domain = email.toLowerCase().split("@")[1];
        
        if (domain.equals("qq.com")) {
            return Providers.QQ;
        } else if (domain.equals("163.com") || domain.equals("126.com") || 
                   domain.equals("yeah.net") || domain.equals("188.com")) {
            return Providers.NETEASE;
        } else if (domain.equals("gmail.com") || domain.endsWith(".googlemail.com")) {
            return Providers.GMAIL;
        } else if (domain.equals("outlook.com") || domain.equals("hotmail.com") || 
                   domain.equals("live.com") || domain.equals("msn.com")) {
            return Providers.OUTLOOK;
        } else if (domain.endsWith(".aliyun.com") || domain.endsWith(".alibaba-inc.com")) {
            return Providers.ALIYUN;
        } else {
            // 对于企业邮箱，默认尝试腾讯企业邮箱
            return Providers.TENCENT;
        }
    }
}