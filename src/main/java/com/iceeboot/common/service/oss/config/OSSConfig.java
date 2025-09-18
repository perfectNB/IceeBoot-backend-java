package com.iceeboot.common.service.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OSS配置类
 * 用于配置各个厂商的OSS服务参数
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OSSConfig {
    
    /**
     * 默认厂商
     */
    private String defaultProvider = "aliyun";
    
    /**
     * 是否启用OSS服务
     */
    private Boolean enabled = true;
    
    /**
     * 全局配置
     */
    private GlobalConfig global = new GlobalConfig();
    
    /**
     * 阿里云OSS配置
     */
    private AliyunConfig aliyun = new AliyunConfig();
    
    /**
     * 华为云OBS配置
     */
    private HuaweiConfig huawei = new HuaweiConfig();
    
    /**
     * 腾讯云COS配置
     */
    private TencentConfig tencent = new TencentConfig();
    
    /**
     * 七牛云Kodo配置
     */
    private QiniuConfig qiniu = new QiniuConfig();
    
    /**
     * MinIO配置
     */
    private MinioConfig minio = new MinioConfig();
    
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
         * 是否使用HTTPS
         */
        private Boolean useHttps = true;
        
        /**
         * 最大文件大小（字节）
         */
        private Long maxFileSize = 100 * 1024 * 1024L; // 100MB
        
        /**
         * 允许的文件类型
         */
        private String[] allowedFileTypes = {"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx", "txt"};
        
        /**
         * 是否启用文件类型检查
         */
        private Boolean enableFileTypeCheck = true;
        
        /**
         * 默认内容类型
         */
        private String defaultContentType = "application/octet-stream";
    }
    
    /**
     * 阿里云OSS配置类
     */
    @Data
    public static class AliyunConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * Access Key ID
         */
        private String accessKeyId;
        
        /**
         * Access Key Secret
         */
        private String accessKeySecret;
        
        /**
         * 端点
         */
        private String endpoint = "oss-cn-hangzhou.aliyuncs.com";
        
        /**
         * 默认存储桶
         */
        private String defaultBucket;
        
        /**
         * 区域
         */
        private String region = "cn-hangzhou";
        
        /**
         * 自定义域名
         */
        private String customDomain;
        
        /**
         * 扩展配置
         */
        private Map<String, Object> extraConfig;
    }
    
    /**
     * 华为云OBS配置类
     */
    @Data
    public static class HuaweiConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * Access Key ID
         */
        private String accessKeyId;
        
        /**
         * Access Key Secret
         */
        private String accessKeySecret;
        
        /**
         * 端点
         */
        private String endpoint = "obs.cn-north-4.myhuaweicloud.com";
        
        /**
         * 默认存储桶
         */
        private String defaultBucket;
        
        /**
         * 区域
         */
        private String region = "cn-north-4";
        
        /**
         * 自定义域名
         */
        private String customDomain;
        
        /**
         * 扩展配置
         */
        private Map<String, Object> extraConfig;
    }
    
    /**
     * 腾讯云COS配置类
     */
    @Data
    public static class TencentConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * Secret ID
         */
        private String secretId;
        
        /**
         * Secret Key
         */
        private String secretKey;
        
        /**
         * 端点
         */
        private String endpoint = "cos.ap-beijing.myqcloud.com";
        
        /**
         * 默认存储桶
         */
        private String defaultBucket;
        
        /**
         * 区域
         */
        private String region = "ap-beijing";
        
        /**
         * APP ID
         */
        private String appId;
        
        /**
         * 自定义域名
         */
        private String customDomain;
        
        /**
         * 扩展配置
         */
        private Map<String, Object> extraConfig;
    }
    
    /**
     * 七牛云Kodo配置类
     */
    @Data
    public static class QiniuConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * Access Key
         */
        private String accessKey;
        
        /**
         * Secret Key
         */
        private String secretKey;
        
        /**
         * 上传端点
         */
        private String uploadEndpoint = "https://upload.qiniup.com";
        
        /**
         * 下载端点
         */
        private String downloadEndpoint = "https://iovip.qbox.me";
        
        /**
         * API端点
         */
        private String apiEndpoint = "https://rs.qbox.me";
        
        /**
         * 默认存储桶
         */
        private String defaultBucket;
        
        /**
         * 区域
         */
        private String region = "z0";
        
        /**
         * 自定义域名
         */
        private String customDomain;
        
        /**
         * 扩展配置
         */
        private Map<String, Object> extraConfig;
    }
    
    /**
     * MinIO配置类
     */
    @Data
    public static class MinioConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * Access Key
         */
        private String accessKey;
        
        /**
         * Secret Key
         */
        private String secretKey;
        
        /**
         * 端点
         */
        private String endpoint = "localhost:9000";
        
        /**
         * 默认存储桶
         */
        private String defaultBucket;
        
        /**
         * 区域
         */
        private String region = "us-east-1";
        
        /**
         * 自定义域名
         */
        private String customDomain;
        
        /**
         * 扩展配置
         */
        private Map<String, Object> extraConfig;
    }
    
    /**
     * 根据厂商名称获取对应的配置
     * 
     * @param provider 厂商名称
     * @return 厂商配置
     */
    public Object getProviderConfig(String provider) {
        if (provider == null) {
            return null;
        }
        
        switch (provider.toLowerCase().trim()) {
            case "aliyun":
                return aliyun;
            case "huawei":
                return huawei;
            case "tencent":
                return tencent;
            case "qiniu":
                return qiniu;
            case "minio":
                return minio;
            default:
                return null;
        }
    }
    
    /**
     * 检查厂商是否启用
     * 
     * @param provider 厂商名称
     * @return 是否启用
     */
    public boolean isProviderEnabled(String provider) {
        if (!enabled) {
            return false;
        }
        
        Object config = getProviderConfig(provider);
        if (config == null) {
            return false;
        }
        
        try {
            // 使用反射获取enabled字段
            java.lang.reflect.Field enabledField = config.getClass().getDeclaredField("enabled");
            enabledField.setAccessible(true);
            Boolean providerEnabled = (Boolean) enabledField.get(config);
            return Boolean.TRUE.equals(providerEnabled);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取厂商的默认存储桶
     * 
     * @param provider 厂商名称
     * @return 默认存储桶
     */
    public String getDefaultBucket(String provider) {
        Object config = getProviderConfig(provider);
        if (config == null) {
            return null;
        }
        
        try {
            // 使用反射获取defaultBucket字段
            java.lang.reflect.Field bucketField = config.getClass().getDeclaredField("defaultBucket");
            bucketField.setAccessible(true);
            return (String) bucketField.get(config);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取厂商的端点
     * 
     * @param provider 厂商名称
     * @return 端点
     */
    public String getEndpoint(String provider) {
        Object config = getProviderConfig(provider);
        if (config == null) {
            return null;
        }
        
        try {
            // 使用反射获取endpoint字段
            java.lang.reflect.Field endpointField = config.getClass().getDeclaredField("endpoint");
            endpointField.setAccessible(true);
            return (String) endpointField.get(config);
        } catch (Exception e) {
            return null;
        }
    }
}