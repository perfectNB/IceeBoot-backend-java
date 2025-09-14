/**
 * SMS工厂类
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.zboot.common.service.sms.factory;

import com.zboot.common.exception.ZbootException;
import com.zboot.common.service.sms.service.SMSService;
import com.zboot.common.service.sms.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SMS工厂类
 * 用于根据厂商名称获取对应的SMS服务实例
 * 采用策略模式 + 工厂模式实现多厂商SMS服务的统一管理
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
@Component
public class SMSFactory {
    
    /**
     * SMS服务实例缓存
     * 使用ConcurrentHashMap保证线程安全
     */
    private static final Map<String, SMSService> SMS_SERVICE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 支持的SMS厂商常量
     */
    public static final class Providers {
        public static final String ALIYUN = "aliyun";
        public static final String TENCENT = "tencent";
        public static final String HUAWEI = "huawei";
        public static final String NETEASE = "netease";
        public static final String RONGLIAN = "ronglian";
        public static final String QINIU = "qiniu";
    }
    
    /**
     * 根据厂商名称获取SMS服务实例
     * 
     * @param provider 厂商名称
     * @return SMS服务实例
     * @throws ZbootException 当厂商名称不支持时抛出异常
     */
    public static SMSService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new ZbootException("SMS厂商名称不能为空");
        }
        
        String normalizedProvider = provider.toLowerCase().trim();
        
        // 从缓存中获取服务实例
        SMSService service = SMS_SERVICE_CACHE.get(normalizedProvider);
        if (service != null) {
            return service;
        }
        
        // 创建新的服务实例
        service = createService(normalizedProvider);
        if (service != null) {
            // 缓存服务实例
            SMS_SERVICE_CACHE.put(normalizedProvider, service);
            log.info("创建SMS服务实例: {}", normalizedProvider);
        }
        
        return service;
    }
    
    /**
     * 根据厂商名称创建SMS服务实例
     * 
     * @param provider 厂商名称
     * @return SMS服务实例
     */
    private static SMSService createService(String provider) {
        switch (provider) {
            case Providers.ALIYUN:
                return new AliyunSMSService();
            case Providers.TENCENT:
                return new TencentSMSService();
            case Providers.HUAWEI:
                return new HuaweiSMSService();
            case Providers.NETEASE:
                return new NeteaseSMSService();
            case Providers.RONGLIAN:
                return new RongLianSMSService();
            case Providers.QINIU:
                return new QiniuSMSService();
            default:
                log.error("不支持的SMS厂商: {}", provider);
                throw new ZbootException("不支持的SMS厂商: " + provider);
        }
    }
    
    /**
     * 获取所有支持的厂商列表
     * 
     * @return 支持的厂商列表
     */
    public static String[] getSupportedProviders() {
        return new String[]{
                Providers.ALIYUN,
                Providers.TENCENT,
                Providers.HUAWEI,
                Providers.NETEASE,
                Providers.RONGLIAN,
                Providers.QINIU
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
            SMS_SERVICE_CACHE.remove(normalizedProvider);
            log.info("清除SMS服务实例缓存: {}", normalizedProvider);
        }
    }
    
    /**
     * 清除所有服务实例缓存
     */
    public static void clearAllCache() {
        SMS_SERVICE_CACHE.clear();
        log.info("清除所有SMS服务实例缓存");
    }
    
    /**
     * 获取当前缓存的服务实例数量
     * 
     * @return 缓存的服务实例数量
     */
    public static int getCacheSize() {
        return SMS_SERVICE_CACHE.size();
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
            case Providers.ALIYUN:
                return "阿里云短信";
            case Providers.TENCENT:
                return "腾讯云短信";
            case Providers.HUAWEI:
                return "华为云短信";
            case Providers.NETEASE:
                return "网易云信";
            case Providers.RONGLIAN:
                return "容联云";
            case Providers.QINIU:
                return "七牛云短信";
            default:
                return provider;
        }
    }
    
    /**
     * 获取厂商的默认端点
     * 
     * @param provider 厂商名称
     * @return 默认端点
     */
    public static String getDefaultEndpoint(String provider) {
        if (provider == null) {
            return null;
        }
        
        switch (provider.toLowerCase().trim()) {
            case Providers.ALIYUN:
                return "https://dysmsapi.aliyuncs.com";
            case Providers.TENCENT:
                return "https://sms.tencentcloudapi.com";
            case Providers.HUAWEI:
                return "https://smsapi.cn-north-4.myhuaweicloud.com:443";
            case Providers.NETEASE:
                return "https://api.netease.im";
            case Providers.RONGLIAN:
                return "https://app.cloopen.com:8883";
            case Providers.QINIU:
                return "https://sms.qiniuapi.com";
            default:
                return null;
        }
    }
    
    /**
     * 获取厂商支持的短信类型
     * 
     * @param provider 厂商名称
     * @return 支持的短信类型列表
     */
    public static String[] getSupportedSMSTypes(String provider) {
        if (provider == null) {
            return new String[0];
        }
        
        // 所有厂商都支持的基本短信类型
        String[] basicTypes = {"verification", "notification", "marketing"};
        
        switch (provider.toLowerCase().trim()) {
            case Providers.ALIYUN:
            case Providers.TENCENT:
            case Providers.HUAWEI:
            case Providers.NETEASE:
            case Providers.RONGLIAN:
            case Providers.QINIU:
                return basicTypes;
            default:
                return basicTypes;
        }
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
        
        switch (provider.toLowerCase().trim()) {
            case Providers.ALIYUN:
                return new String[]{
                        "single", "batch", "template", "verification", "notification", "marketing", "status_query"
                };
            case Providers.TENCENT:
                return new String[]{
                        "single", "batch", "template", "verification", "notification", "marketing", "status_query", "balance_query"
                };
            case Providers.HUAWEI:
                return new String[]{
                        "single", "batch", "template", "verification", "notification", "marketing", "status_query", "balance_query"
                };
            case Providers.NETEASE:
                return new String[]{
                        "single", "batch", "template", "verification", "notification", "marketing", "status_query"
                };
            case Providers.RONGLIAN:
                return new String[]{
                        "single", "batch", "template", "verification", "notification", "marketing", "status_query", "balance_query"
                };
            case Providers.QINIU:
                return new String[]{
                        "single", "batch", "template", "verification", "notification", "marketing", "status_query"
                };
            default:
                return new String[]{"single", "template"};
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
            case Providers.ALIYUN:
                return "需要AccessKeyId和AccessKeySecret";
            case Providers.TENCENT:
                return "需要SecretId、SecretKey和SmsSdkAppId";
            case Providers.HUAWEI:
                return "需要AppKey、AppSecret和通道号";
            case Providers.NETEASE:
                return "需要AppKey和AppSecret";
            case Providers.RONGLIAN:
                return "需要AccountSid、AuthToken和AppId";
            case Providers.QINIU:
                return "需要AccessKey和SecretKey";
            default:
                return "请查看对应厂商文档";
        }
    }
}