/**
 * OSS工厂类
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.oss.factory;

import com.zboot.common.service.oss.service.OSSService;
import com.zboot.common.service.oss.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSS工厂类
 * 用于根据厂商名称获取对应的OSS服务实例
 * 采用策略模式 + 工厂模式实现多厂商OSS服务的统一管理
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class OSSFactory {
    
    /**
     * OSS服务实例缓存
     * 使用ConcurrentHashMap保证线程安全
     */
    private static final Map<String, OSSService> OSS_SERVICE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 支持的OSS厂商常量
     */
    public static final class Providers {
        public static final String ALIYUN = "aliyun";
        public static final String HUAWEI = "huawei";
        public static final String TENCENT = "tencent";
        public static final String QINIU = "qiniu";
        public static final String MINIO = "minio";
    }
    
    /**
     * 根据厂商名称获取OSS服务实例
     * 
     * @param provider 厂商名称
     * @return OSS服务实例
     * @throws IllegalArgumentException 当厂商名称不支持时抛出异常
     */
    public static OSSService getService(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("OSS厂商名称不能为空");
        }
        
        String normalizedProvider = provider.toLowerCase().trim();
        
        // 从缓存中获取服务实例
        OSSService service = OSS_SERVICE_CACHE.get(normalizedProvider);
        if (service != null) {
            return service;
        }
        
        // 创建新的服务实例
        service = createService(normalizedProvider);
        if (service != null) {
            // 缓存服务实例
            OSS_SERVICE_CACHE.put(normalizedProvider, service);
            log.info("创建OSS服务实例: {}", normalizedProvider);
        }
        
        return service;
    }
    
    /**
     * 根据厂商名称创建OSS服务实例
     * 
     * @param provider 厂商名称
     * @return OSS服务实例
     */
    private static OSSService createService(String provider) {
        switch (provider) {
            case Providers.ALIYUN:
                return new AliyunOSSService();
            case Providers.HUAWEI:
                return new HuaweiOBSService();
            case Providers.TENCENT:
                return new TencentCOSService();
            case Providers.QINIU:
                return new QiniuKodoService();
            case Providers.MINIO:
                return new MinIOService();
            default:
                log.error("不支持的OSS厂商: {}", provider);
                throw new IllegalArgumentException("不支持的OSS厂商: " + provider);
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
                Providers.HUAWEI,
                Providers.TENCENT,
                Providers.QINIU,
                Providers.MINIO
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
            OSS_SERVICE_CACHE.remove(normalizedProvider);
            log.info("清除OSS服务实例缓存: {}", normalizedProvider);
        }
    }
    
    /**
     * 清除所有服务实例缓存
     */
    public static void clearAllCache() {
        OSS_SERVICE_CACHE.clear();
        log.info("清除所有OSS服务实例缓存");
    }
    
    /**
     * 获取当前缓存的服务实例数量
     * 
     * @return 缓存的服务实例数量
     */
    public static int getCacheSize() {
        return OSS_SERVICE_CACHE.size();
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
                return "阿里云OSS";
            case Providers.HUAWEI:
                return "华为云OBS";
            case Providers.TENCENT:
                return "腾讯云COS";
            case Providers.QINIU:
                return "七牛云Kodo";
            case Providers.MINIO:
                return "MinIO";
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
                return "oss-cn-hangzhou.aliyuncs.com";
            case Providers.HUAWEI:
                return "obs.cn-north-4.myhuaweicloud.com";
            case Providers.TENCENT:
                return "cos.ap-beijing.myqcloud.com";
            case Providers.QINIU:
                return "upload.qiniup.com";
            case Providers.MINIO:
                return "localhost:9000";
            default:
                return null;
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
        
        // 所有厂商都支持的基本功能
        String[] basicFeatures = {
                "upload", "download", "delete", "list", "capacity", "exists", "copy", "move"
        };
        
        switch (provider.toLowerCase().trim()) {
            case Providers.ALIYUN:
            case Providers.HUAWEI:
            case Providers.TENCENT:
            case Providers.MINIO:
                // 这些厂商支持完整的S3兼容功能
                return new String[]{
                        "upload", "download", "delete", "list", "capacity", "exists", 
                        "copy", "move", "presigned", "bucket_create", "bucket_delete"
                };
            case Providers.QINIU:
                // 七牛云有一些特殊限制
                return new String[]{
                        "upload", "download", "delete", "list", "capacity", "exists", 
                        "copy", "move", "presigned"
                };
            default:
                return basicFeatures;
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
}