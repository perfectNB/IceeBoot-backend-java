/**
 * 数据权限异常
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
package com.iceeboot.common.exception;

/**
 * 数据权限异常
 * 当用户尝试访问不属于自己的资源时抛出
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
public class DataPermissionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code = 403;

    public DataPermissionException() {
        super("您没有权限访问此资源");
    }

    public DataPermissionException(String message) {
        super(message);
    }

    public DataPermissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataPermissionException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}

