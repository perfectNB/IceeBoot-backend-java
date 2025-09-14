/**
 * SMS请求DTO
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.iceeboot.common.service.sms.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * SMS请求DTO
 * @author CodeIcee
 * @date 2025-01-24
 */
@Data
@Accessors(chain = true)
public class SMSRequest {
    
    /**
     * 手机号码
     */
    private String phoneNumber;
    
    /**
     * 短信内容
     */
    private String content;
    
    /**
     * 短信模板ID
     */
    private String templateId;
    
    /**
     * 短信模板参数
     */
    private Map<String, String> templateParams;
    
    /**
     * 短信签名
     */
    private String signName;
    
    /**
     * 访问密钥ID
     */
    private String accessKeyId;
    
    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;
    
    /**
     * 应用ID（部分厂商需要）
     */
    private String appId;
    
    /**
     * 应用Key（部分厂商需要）
     */
    private String appKey;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 端点URL
     */
    private String endpoint;
    
    /**
     * 是否使用HTTPS
     */
    private Boolean useHttps;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout;
    
    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 短信类型（验证码、通知、营销等）
     */
    private String smsType;
    
    /**
     * 国际区号（国际短信）
     */
    private String countryCode;
    
    /**
     * 批量发送的手机号列表
     */
    private String[] phoneNumbers;
    
    /**
     * 定时发送时间（时间戳）
     */
    private Long scheduleTime;
    
    /**
     * 短信有效期（分钟）
     */
    private Integer validityPeriod;
    
    /**
     * 回调URL
     */
    private String callbackUrl;
    
    /**
     * 用户自定义数据
     */
    private String userData;
}