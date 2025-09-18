package com.iceeboot.common.service.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * SMS配置类
 * 支持多厂商SMS服务的统一配置管理
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SMSConfig {
    
    /**
     * 是否启用SMS功能
     */
    private Boolean enabled = true;
    
    /**
     * 默认SMS厂商
     */
    private String defaultProvider = "aliyun";
    
    /**
     * 全局配置
     */
    private GlobalConfig global = new GlobalConfig();
    
    /**
     * 阿里云SMS配置
     */
    private AliyunConfig aliyun = new AliyunConfig();
    
    /**
     * 腾讯云SMS配置
     */
    private TencentConfig tencent = new TencentConfig();
    
    /**
     * 华为云SMS配置
     */
    private HuaweiConfig huawei = new HuaweiConfig();
    
    /**
     * 网易云信SMS配置
     */
    private NeteaseConfig netease = new NeteaseConfig();
    
    /**
     * 容联云SMS配置
     */
    private RongLianConfig ronglian = new RongLianConfig();
    
    /**
     * 七牛云SMS配置
     */
    private QiniuConfig qiniu = new QiniuConfig();
    
    /**
     * 全局配置类
     */
    @Data
    public static class GlobalConfig {
        /**
         * 默认超时时间（秒）
         */
        private Integer timeout = 30;
        
        /**
         * 是否使用HTTPS
         */
        private Boolean useHttps = true;
        
        /**
         * 默认短信类型
         */
        private String defaultSmsType = "notification";
        
        /**
         * 最大重试次数
         */
        private Integer maxRetries = 3;
        
        /**
         * 重试间隔（毫秒）
         */
        private Long retryInterval = 1000L;
        
        /**
         * 是否启用日志记录
         */
        private Boolean enableLogging = true;
        
        /**
         * 短信内容最大长度
         */
        private Integer maxContentLength = 500;
        
        /**
         * 批量发送最大数量
         */
        private Integer maxBatchSize = 100;
    }
    
    /**
     * 阿里云SMS配置
     */
    @Data
    public static class AliyunConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * AccessKey ID
         */
        private String accessKeyId;
        
        /**
         * AccessKey Secret
         */
        private String accessKeySecret;
        
        /**
         * 区域
         */
        private String region = "cn-hangzhou";
        
        /**
         * 端点
         */
        private String endpoint = "https://dysmsapi.aliyuncs.com";
        
        /**
         * 默认签名
         */
        private String defaultSignName;
        
        /**
         * 默认模板ID
         */
        private String defaultTemplateId;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
    }
    
    /**
     * 腾讯云SMS配置
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
         * SMS SDK App ID
         */
        private String smsSdkAppId;
        
        /**
         * 区域
         */
        private String region = "ap-beijing";
        
        /**
         * 端点
         */
        private String endpoint = "https://sms.tencentcloudapi.com";
        
        /**
         * 默认签名
         */
        private String defaultSignName;
        
        /**
         * 默认模板ID
         */
        private String defaultTemplateId;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
    }
    
    /**
     * 华为云SMS配置
     */
    @Data
    public static class HuaweiConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * App Key
         */
        private String appKey;
        
        /**
         * App Secret
         */
        private String appSecret;
        
        /**
         * 通道号
         */
        private String sender;
        
        /**
         * 区域
         */
        private String region = "cn-north-4";
        
        /**
         * 端点
         */
        private String endpoint = "https://smsapi.cn-north-4.myhuaweicloud.com:443";
        
        /**
         * 默认签名
         */
        private String defaultSignName;
        
        /**
         * 默认模板ID
         */
        private String defaultTemplateId;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
    }
    
    /**
     * 网易云信SMS配置
     */
    @Data
    public static class NeteaseConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * App Key
         */
        private String appKey;
        
        /**
         * App Secret
         */
        private String appSecret;
        
        /**
         * 端点
         */
        private String endpoint = "https://api.netease.im";
        
        /**
         * 默认模板ID
         */
        private String defaultTemplateId;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
    }
    
    /**
     * 容联云SMS配置
     */
    @Data
    public static class RongLianConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        
        /**
         * Account SID
         */
        private String accountSid;
        
        /**
         * Auth Token
         */
        private String authToken;
        
        /**
         * App ID
         */
        private String appId;
        
        /**
         * 端点
         */
        private String endpoint = "https://app.cloopen.com:8883";
        
        /**
         * 默认模板ID
         */
        private String defaultTemplateId;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
    }
    
    /**
     * 七牛云SMS配置
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
         * 端点
         */
        private String endpoint = "https://sms.qiniuapi.com";
        
        /**
         * 默认模板ID
         */
        private String defaultTemplateId;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
    }
    
    /**
     * 根据厂商名称获取对应的配置
     * 
     * @param provider 厂商名称
     * @return 厂商配置对象
     */
    public Object getProviderConfig(String provider) {
        if (provider == null) {
            return null;
        }
        
        switch (provider.toLowerCase().trim()) {
            case "aliyun":
                return this.aliyun;
            case "tencent":
                return this.tencent;
            case "huawei":
                return this.huawei;
            case "netease":
                return this.netease;
            case "ronglian":
                return this.ronglian;
            case "qiniu":
                return this.qiniu;
            default:
                return null;
        }
    }
    
    /**
     * 检查指定厂商是否启用
     * 
     * @param provider 厂商名称
     * @return 是否启用
     */
    public Boolean isProviderEnabled(String provider) {
        Object config = getProviderConfig(provider);
        if (config == null) {
            return false;
        }
        
        try {
            Field enabledField = config.getClass().getDeclaredField("enabled");
            enabledField.setAccessible(true);
            Boolean enabled = (Boolean) enabledField.get(config);
            return enabled != null && enabled;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取指定厂商的默认端点
     * 
     * @param provider 厂商名称
     * @return 默认端点
     */
    public String getProviderEndpoint(String provider) {
        Object config = getProviderConfig(provider);
        if (config == null) {
            return null;
        }
        
        try {
            Field endpointField = config.getClass().getDeclaredField("endpoint");
            endpointField.setAccessible(true);
            return (String) endpointField.get(config);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取指定厂商的默认超时时间
     * 
     * @param provider 厂商名称
     * @return 默认超时时间
     */
    public Integer getProviderTimeout(String provider) {
        Object config = getProviderConfig(provider);
        if (config == null) {
            return this.global.getTimeout();
        }
        
        try {
            Field timeoutField = config.getClass().getDeclaredField("timeout");
            timeoutField.setAccessible(true);
            Integer timeout = (Integer) timeoutField.get(config);
            return timeout != null ? timeout : this.global.getTimeout();
        } catch (Exception e) {
            return this.global.getTimeout();
        }
    }
    
    /**
     * 获取指定厂商的默认签名
     * 
     * @param provider 厂商名称
     * @return 默认签名
     */
    public String getProviderDefaultSignName(String provider) {
        Object config = getProviderConfig(provider);
        if (config == null) {
            return null;
        }
        
        try {
            Field signNameField = config.getClass().getDeclaredField("defaultSignName");
            signNameField.setAccessible(true);
            return (String) signNameField.get(config);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取指定厂商的默认模板ID
     * 
     * @param provider 厂商名称
     * @return 默认模板ID
     */
    public String getProviderDefaultTemplateId(String provider) {
        Object config = getProviderConfig(provider);
        if (config == null) {
            return null;
        }
        
        try {
            Field templateIdField = config.getClass().getDeclaredField("defaultTemplateId");
            templateIdField.setAccessible(true);
            return (String) templateIdField.get(config);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取所有启用的厂商列表
     * 
     * @return 启用的厂商列表
     */
    public String[] getEnabledProviders() {
        String[] allProviders = {"aliyun", "tencent", "huawei", "netease", "ronglian", "qiniu"};
        return java.util.Arrays.stream(allProviders)
                .filter(this::isProviderEnabled)
                .toArray(String[]::new);
    }
    
    /**
     * 获取配置摘要信息
     * 
     * @return 配置摘要
     */
    public Map<String, Object> getConfigSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("enabled", this.enabled);
        summary.put("defaultProvider", this.defaultProvider);
        summary.put("enabledProviders", getEnabledProviders());
        summary.put("globalTimeout", this.global.getTimeout());
        summary.put("maxContentLength", this.global.getMaxContentLength());
        summary.put("maxBatchSize", this.global.getMaxBatchSize());
        return summary;
    }
}