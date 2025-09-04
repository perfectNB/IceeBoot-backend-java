/**
 * SMS异常处理类
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.zboot.common.service.sms.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SMS异常处理类
 * 用于处理SMS操作过程中的各种异常情况
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SMSException extends RuntimeException {
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * SMS厂商
     */
    private String provider;
    
    /**
     * 操作类型
     */
    private String operation;
    
    /**
     * 手机号码
     */
    private String phoneNumber;
    
    /**
     * 短信ID
     */
    private String messageId;
    
    /**
     * 模板ID
     */
    private String templateId;
    
    /**
     * 异常发生时间戳
     */
    private Long timestamp;
    
    /**
     * 默认构造函数
     */
    public SMSException() {
        super();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public SMSException(String message) {
        super(message);
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public SMSException(String message, Throwable cause) {
        super(message, cause);
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 异常消息
     */
    public SMSException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 异常消息
     * @param provider SMS厂商
     */
    public SMSException(String errorCode, String message, String provider) {
        super(message);
        this.errorCode = errorCode;
        this.provider = provider;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 异常消息
     * @param provider SMS厂商
     * @param operation 操作类型
     */
    public SMSException(String errorCode, String message, String provider, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.provider = provider;
        this.operation = operation;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取详细的异常信息
     * 
     * @return 详细异常信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("SMS异常: ");
        
        if (errorCode != null) {
            sb.append("[错误代码: ").append(errorCode).append("] ");
        }
        
        if (provider != null) {
            sb.append("[厂商: ").append(provider).append("] ");
        }
        
        if (operation != null) {
            sb.append("[操作: ").append(operation).append("] ");
        }
        
        if (phoneNumber != null) {
            sb.append("[手机号: ").append(phoneNumber).append("] ");
        }
        
        if (messageId != null) {
            sb.append("[消息ID: ").append(messageId).append("] ");
        }
        
        if (templateId != null) {
            sb.append("[模板ID: ").append(templateId).append("] ");
        }
        
        sb.append(getMessage());
        
        return sb.toString();
    }
    
    /**
     * SMS异常工厂类
     * 提供常用异常的快速创建方法
     */
    public static class Factory {
        
        /**
         * 创建发送失败异常
         */
        public static SMSException sendFailed(String provider, String phoneNumber, String message) {
            return new SMSException("SEND_FAILED", message, provider, "send")
                    .setPhoneNumber(phoneNumber);
        }
        
        /**
         * 创建发送失败异常
         */
        public static SMSException sendFailed(String provider, String phoneNumber, String message, Throwable cause) {
            SMSException exception = new SMSException(message, cause);
            exception.setErrorCode("SEND_FAILED");
            exception.setProvider(provider);
            exception.setOperation("send");
            exception.setPhoneNumber(phoneNumber);
            return exception;
        }
        
        /**
         * 创建批量发送失败异常
         */
        public static SMSException batchSendFailed(String provider, String message) {
            return new SMSException("BATCH_SEND_FAILED", message, provider, "batch_send");
        }
        
        /**
         * 创建查询失败异常
         */
        public static SMSException queryFailed(String provider, String messageId, String message) {
            return new SMSException("QUERY_FAILED", message, provider, "query")
                    .setMessageId(messageId);
        }
        
        /**
         * 创建余额查询失败异常
         */
        public static SMSException balanceQueryFailed(String provider, String message) {
            return new SMSException("BALANCE_QUERY_FAILED", message, provider, "balance_query");
        }
        
        /**
         * 创建配置错误异常
         */
        public static SMSException configError(String provider, String message) {
            return new SMSException("CONFIG_ERROR", message, provider, "config");
        }
        
        /**
         * 创建认证失败异常
         */
        public static SMSException authenticationFailed(String provider, String message) {
            return new SMSException("AUTHENTICATION_FAILED", message, provider, "auth");
        }
        
        /**
         * 创建权限不足异常
         */
        public static SMSException accessDenied(String provider, String message) {
            return new SMSException("ACCESS_DENIED", message, provider, "access");
        }
        
        /**
         * 创建网络错误异常
         */
        public static SMSException networkError(String provider, String message) {
            return new SMSException("NETWORK_ERROR", message, provider, "network");
        }
        
        /**
         * 创建网络错误异常
         */
        public static SMSException networkError(String provider, String message, Throwable cause) {
            SMSException exception = new SMSException(message, cause);
            exception.setErrorCode("NETWORK_ERROR");
            exception.setProvider(provider);
            exception.setOperation("network");
            return exception;
        }
        
        /**
         * 创建超时异常
         */
        public static SMSException timeout(String provider, String message) {
            return new SMSException("TIMEOUT", message, provider, "timeout");
        }
        
        /**
         * 创建参数错误异常
         */
        public static SMSException invalidParameter(String provider, String paramName, String message) {
            return new SMSException("INVALID_PARAMETER", 
                    String.format("参数错误 [%s]: %s", paramName, message), provider, "validation");
        }
        
        /**
         * 创建手机号格式错误异常
         */
        public static SMSException invalidPhoneNumber(String provider, String phoneNumber) {
            return new SMSException("INVALID_PHONE_NUMBER", 
                    "手机号格式错误: " + phoneNumber, provider, "validation")
                    .setPhoneNumber(phoneNumber);
        }
        
        /**
         * 创建短信内容过长异常
         */
        public static SMSException contentTooLong(String provider, int maxLength, int actualLength) {
            return new SMSException("CONTENT_TOO_LONG", 
                    String.format("短信内容过长，最大长度: %d，实际长度: %d", maxLength, actualLength), 
                    provider, "validation");
        }
        
        /**
         * 创建模板不存在异常
         */
        public static SMSException templateNotFound(String provider, String templateId) {
            return new SMSException("TEMPLATE_NOT_FOUND", 
                    "短信模板不存在: " + templateId, provider, "template")
                    .setTemplateId(templateId);
        }
        
        /**
         * 创建签名不存在异常
         */
        public static SMSException signatureNotFound(String provider, String signName) {
            return new SMSException("SIGNATURE_NOT_FOUND", 
                    "短信签名不存在: " + signName, provider, "signature");
        }
        
        /**
         * 创建余额不足异常
         */
        public static SMSException insufficientBalance(String provider) {
            return new SMSException("INSUFFICIENT_BALANCE", "账户余额不足", provider, "balance");
        }
        
        /**
         * 创建频率限制异常
         */
        public static SMSException rateLimitExceeded(String provider, String phoneNumber) {
            return new SMSException("RATE_LIMIT_EXCEEDED", 
                    "发送频率超限: " + phoneNumber, provider, "rate_limit")
                    .setPhoneNumber(phoneNumber);
        }
        
        /**
         * 创建不支持的厂商异常
         */
        public static SMSException unsupportedProvider(String provider) {
            return new SMSException("UNSUPPORTED_PROVIDER", 
                    "不支持的SMS厂商: " + provider, provider, "provider");
        }
        
        /**
         * 创建不支持的操作异常
         */
        public static SMSException unsupportedOperation(String provider, String operation) {
            return new SMSException("UNSUPPORTED_OPERATION", 
                    String.format("厂商 %s 不支持操作: %s", provider, operation), provider, operation);
        }
        
        /**
         * 创建解析响应失败异常
         */
        public static SMSException parseResponseFailed(String provider, String message) {
            return new SMSException("PARSE_RESPONSE_FAILED", message, provider, "parse");
        }
        
        /**
         * 创建解析响应失败异常
         */
        public static SMSException parseResponseFailed(String provider, String message, Throwable cause) {
            SMSException exception = new SMSException(message, cause);
            exception.setErrorCode("PARSE_RESPONSE_FAILED");
            exception.setProvider(provider);
            exception.setOperation("parse");
            return exception;
        }
        
        /**
         * 创建服务不可用异常
         */
        public static SMSException serviceUnavailable(String provider, String message) {
            return new SMSException("SERVICE_UNAVAILABLE", message, provider, "service");
        }
        
        /**
         * 创建内部错误异常
         */
        public static SMSException internalError(String provider, String message) {
            return new SMSException("INTERNAL_ERROR", message, provider, "internal");
        }
        
        /**
         * 创建内部错误异常
         */
        public static SMSException internalError(String provider, String message, Throwable cause) {
            SMSException exception = new SMSException(message, cause);
            exception.setErrorCode("INTERNAL_ERROR");
            exception.setProvider(provider);
            exception.setOperation("internal");
            return exception;
        }
    }
    
    /**
     * 设置手机号码（链式调用）
     */
    public SMSException setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }
    
    /**
     * 设置短信ID（链式调用）
     */
    public SMSException setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }
    
    /**
     * 设置模板ID（链式调用）
     */
    public SMSException setTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }
    
    /**
     * 设置错误代码（链式调用）
     */
    public SMSException setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }
    
    /**
     * 设置厂商（链式调用）
     */
    public SMSException setProvider(String provider) {
        this.provider = provider;
        return this;
    }
    
    /**
     * 设置操作类型（链式调用）
     */
    public SMSException setOperation(String operation) {
        this.operation = operation;
        return this;
    }
}