package com.iceeboot.common.exception;

/**
 * 业务异常
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
public class ServiceException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private Integer code;
    
    /**
     * 错误提示
     */
    private String message;
    
    /**
     * 错误明细，内部调试错误
     */
    private String detailMessage;
    
    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException() {
    }
    
    public ServiceException(String message) {
        this.message = message;
    }
    
    public ServiceException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }
    
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
    
    public ServiceException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public ServiceException(Integer code, String message, String detailMessage) {
        this.code = code;
        this.message = message;
        this.detailMessage = detailMessage;
    }
    
    public String getDetailMessage() {
        return detailMessage;
    }
    
    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public ServiceException setCode(Integer code) {
        this.code = code;
        return this;
    }
}