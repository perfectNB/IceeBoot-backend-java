/**
 * SMS响应DTO
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.iceeboot.common.service.sms.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SMS响应DTO
 * @author CodeIcee
 * @date 2025-01-24
 */
@Data
@Accessors(chain = true)
public class SMSResponse {
    
    /**
     * 操作是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 短信ID（厂商返回的唯一标识）
     */
    private String messageId;
    
    /**
     * 批量发送时的消息ID列表
     */
    private String[] messageIds;
    
    /**
     * 手机号码
     */
    private String phoneNumber;
    
    /**
     * 批量发送的手机号列表
     */
    private String[] phoneNumbers;
    
    /**
     * 短信内容
     */
    private String content;
    
    /**
     * 短信状态
     */
    private String status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 厂商名称
     */
    private String provider;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;
    
    /**
     * 短信费用
     */
    private String cost;
    
    /**
     * 短信条数
     */
    private Integer smsCount;
    
    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;
    
    /**
     * 原始响应数据
     */
    private String rawResponse;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 序列号
     */
    private String serialNumber;
    
    /**
     * 创建成功响应
     */
    public static SMSResponse success() {
        return new SMSResponse().setSuccess(true).setSendTime(LocalDateTime.now());
    }
    
    /**
     * 创建失败响应
     */
    public static SMSResponse failure(String errorMessage) {
        return new SMSResponse()
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .setSendTime(LocalDateTime.now());
    }
    
    /**
     * 创建失败响应
     */
    public static SMSResponse failure(String errorCode, String errorMessage) {
        return new SMSResponse()
                .setSuccess(false)
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage)
                .setSendTime(LocalDateTime.now());
    }
}