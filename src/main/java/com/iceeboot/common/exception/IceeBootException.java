/**
 * iceeboot自定义异常
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.common.exception;

/**
 * iceeboot自定义异常
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public class IceeBootException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    public IceeBootException() {
        super();
    }

    public IceeBootException(String message) {
        super(message);
        this.message = message;
    }

    public IceeBootException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public IceeBootException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public IceeBootException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}