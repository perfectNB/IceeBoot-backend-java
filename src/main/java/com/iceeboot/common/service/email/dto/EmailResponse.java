package com.iceeboot.common.service.email.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Email响应DTO
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Accessors(chain = true)
public class EmailResponse {
    
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
     * 邮件ID（服务商返回的唯一标识）
     */
    private String messageId;
    
    /**
     * 批量发送时的消息ID列表
     */
    private List<String> messageIds;
    
    /**
     * 发件人邮箱
     */
    private String fromEmail;
    
    /**
     * 收件人邮箱
     */
    private String toEmail;
    
    /**
     * 批量收件人邮箱列表
     */
    private List<String> toEmails;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 邮件状态
     */
    private String status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 邮件服务提供商
     */
    private String provider;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;
    
    /**
     * 邮件大小（字节）
     */
    private Long emailSize;
    
    /**
     * 附件数量
     */
    private Integer attachmentCount;
    
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
     * 服务器响应码
     */
    private Integer responseCode;
    
    /**
     * 服务器响应消息
     */
    private String responseMessage;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 发送结果详情列表（批量发送时使用）
     */
    private List<EmailSendResult> sendResults;
    
    /**
     * 创建成功响应
     */
    public static EmailResponse success() {
        return new EmailResponse().setSuccess(true).setSendTime(LocalDateTime.now());
    }
    
    /**
     * 创建失败响应
     */
    public static EmailResponse failure(String errorMessage) {
        return new EmailResponse()
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .setSendTime(LocalDateTime.now());
    }
    
    /**
     * 创建失败响应
     */
    public static EmailResponse failure(String errorCode, String errorMessage) {
        return new EmailResponse()
                .setSuccess(false)
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage)
                .setSendTime(LocalDateTime.now());
    }
    
    /**
     * 邮件发送结果内部类
     */
    @Data
    @Accessors(chain = true)
    public static class EmailSendResult {
        /**
         * 收件人邮箱
         */
        private String toEmail;
        
        /**
         * 是否发送成功
         */
        private Boolean success;
        
        /**
         * 消息ID
         */
        private String messageId;
        
        /**
         * 错误信息
         */
        private String errorMessage;
        
        /**
         * 错误代码
         */
        private String errorCode;
        
        /**
         * 发送状态
         */
        private String status;
        
        /**
         * 发送时间
         */
        private LocalDateTime sendTime;
    }
}