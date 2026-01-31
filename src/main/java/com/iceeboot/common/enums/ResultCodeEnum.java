/**
 * 返回结果状态码枚举
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.common.enums;

/**
 * 返回结果状态码枚举
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public enum ResultCodeEnum {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    ERROR(500, "操作失败"),

    /**
     * 参数错误
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请重新登录"),
    
    REFRESH_TOKEN_INVALID(40101, "刷新令牌已失效"),
    
    REFRESH_TOKEN_CONSUMED(40102, "刷新令牌已被使用"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "没有权限访问该资源"),

    /**
     * 资源未找到
     */
    NOT_FOUND(404, "请求的资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 用户名或密码错误
     */
    LOGIN_ERROR(1001, "用户名或密码错误"),

    /**
     * 用户已被禁用
     */
    USER_DISABLED(1002, "用户已被禁用"),

    /**
     * 用户不存在
     */
    USER_NOT_EXIST(1003, "用户不存在"),

    /**
     * 用户名已存在
     */
    USERNAME_EXIST(1004, "用户名已存在"),

    /**
     * 手机号已存在
     */
    PHONE_EXIST(1005, "手机号已存在"),

    /**
     * 邮箱已存在
     */
    EMAIL_EXIST(1006, "邮箱已存在"),

    /**
     * 角色不存在
     */
    ROLE_NOT_EXIST(2001, "角色不存在"),

    /**
     * 角色编码已存在
     */
    ROLE_CODE_EXIST(2002, "角色编码已存在"),

    /**
     * 角色名称已存在
     */
    ROLE_NAME_EXIST(2003, "角色名称已存在"),

    /**
     * 权限不存在
     */
    PERMISSION_NOT_EXIST(3001, "权限不存在"),

    /**
     * 权限编码已存在
     */
    PERMISSION_CODE_EXIST(3002, "权限编码已存在"),

    /**
     * 菜单不存在
     */
    MENU_NOT_EXIST(4001, "菜单不存在"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_ERROR(5001, "文件上传失败"),

    /**
     * 文件类型不支持
     */
    FILE_TYPE_ERROR(5002, "文件类型不支持"),

    /**
     * 文件大小超出限制
     */
    FILE_SIZE_ERROR(5003, "文件大小超出限制"),

    /**
     * 验证码错误
     */
    CAPTCHA_ERROR(6001, "验证码错误"),

    /**
     * 验证码已过期
     */
    CAPTCHA_EXPIRED(6002, "验证码已过期");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
