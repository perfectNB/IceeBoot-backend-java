/**
 * 大模型服务异常类
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.llm.exception;

/**
 * 大模型服务异常类
 * @author CodeIcee
 * @date 2025-09-03
 */
public class LLMException extends RuntimeException {
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 厂商名称
     */
    private String provider;
    
    /**
     * 构造函数
     * @param message 错误消息
     */
    public LLMException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param cause 原因
     */
    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param errorCode 错误代码
     */
    public LLMException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param provider 厂商名称
     */
    public LLMException(String message, String errorCode, String provider) {
        super(message);
        this.errorCode = errorCode;
        this.provider = provider;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param cause 原因
     * @param errorCode 错误代码
     * @param provider 厂商名称
     */
    public LLMException(String message, Throwable cause, String errorCode, String provider) {
        super(message, cause);
        this.errorCode = errorCode;
        this.provider = provider;
    }
    
    /**
     * 获取错误代码
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 获取厂商名称
     * @return 厂商名称
     */
    public String getProvider() {
        return provider;
    }
    
    /**
     * API密钥无效异常
     */
    public static class InvalidApiKeyException extends LLMException {
        public InvalidApiKeyException(String provider) {
            super("API密钥无效", "INVALID_API_KEY", provider);
        }
    }
    
    /**
     * 模型不支持异常
     */
    public static class UnsupportedModelException extends LLMException {
        public UnsupportedModelException(String model, String provider) {
            super("不支持的模型: " + model, "UNSUPPORTED_MODEL", provider);
        }
    }
    
    /**
     * 请求超时异常
     */
    public static class TimeoutException extends LLMException {
        public TimeoutException(String provider) {
            super("请求超时", "TIMEOUT", provider);
        }
    }
    
    /**
     * 配额不足异常
     */
    public static class QuotaExceededException extends LLMException {
        public QuotaExceededException(String provider) {
            super("配额不足", "QUOTA_EXCEEDED", provider);
        }
    }
    
    /**
     * 内容过滤异常
     */
    public static class ContentFilteredException extends LLMException {
        public ContentFilteredException(String provider) {
            super("内容被过滤", "CONTENT_FILTERED", provider);
        }
    }
    
    /**
     * 服务不可用异常
     */
    public static class ServiceUnavailableException extends LLMException {
        public ServiceUnavailableException(String provider) {
            super("服务不可用", "SERVICE_UNAVAILABLE", provider);
        }
    }
    
    /**
     * 参数无效异常
     */
    public static class InvalidParameterException extends LLMException {
        public InvalidParameterException(String parameter, String provider) {
            super("参数无效: " + parameter, "INVALID_PARAMETER", provider);
        }
    }
}