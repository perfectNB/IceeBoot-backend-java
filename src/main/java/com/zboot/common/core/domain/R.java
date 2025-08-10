/*
 * @Description: 统一响应结果类
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.zboot.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zboot.common.enums.ResultCodeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一响应结果类
 * 
 * @author CodeIcee
 * @since 2025-08-11
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final int SUCCESS = 200;

    /**
     * 失败状态码
     */
    public static final int FAIL = 500;

    /**
     * 未授权状态码
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 禁止访问状态码
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源未找到状态码
     */
    public static final int NOT_FOUND = 404;

    /**
     * 请求方法不支持状态码
     */
    public static final int METHOD_NOT_ALLOWED = 405;

    /**
     * 参数错误状态码
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 状态码
     */
    private int code;

    /**
     * 返回内容
     */
    private String message;

    /**
     * 数据对象
     */
    private T result;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 初始化一个新创建的 R 对象，使其表示一个空消息。
     */
    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 初始化一个新创建的 R 对象
     *
     * @param code 状态码
     * @param message 返回内容
     */
    public R(int code, String message) {
        this.code = code;
        this.message = message;
        this.success = SUCCESS == code;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 初始化一个新创建的 R 对象
     *
     * @param code 状态码
     * @param message 返回内容
     * @param result 数据对象
     */
    public R(int code, String message, T result) {
        this.code = code;
        this.message = message;
        this.result = result;
        this.success = SUCCESS == code;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static <T> R<T> ok() {
        return new R<>(SUCCESS, "操作成功");
    }

    /**
     * 返回成功数据
     *
     * @return 成功消息
     */
    public static <T> R<T> ok(T result) {
        return new R<>(SUCCESS, "操作成功", result);
    }

    /**
     * 返回成功消息
     *
     * @param message 返回内容
     * @return 成功消息
     */
    public static <T> R<T> ok(String message) {
        return new R<>(SUCCESS, message);
    }

    /**
     * 返回成功消息
     *
     * @param message 返回内容
     * @param result 数据对象
     * @return 成功消息
     */
    public static <T> R<T> ok(String message, T result) {
        return new R<>(SUCCESS, message, result);
    }

    /**
     * 返回错误消息
     *
     * @return 错误消息
     */
    public static <T> R<T> fail() {
        return new R<>(FAIL, "操作失败");
    }

    /**
     * 返回错误消息
     *
     * @param message 返回内容
     * @return 错误消息
     */
    public static <T> R<T> fail(String message) {
        return new R<>(FAIL, message);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param message 返回内容
     * @return 错误消息
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param message 返回内容
     * @param result 数据对象
     * @return 错误消息
     */
    public static <T> R<T> fail(int code, String message, T result) {
        return new R<>(code, message, result);
    }

    /**
     * 返回错误消息
     *
     * @param resultCodeEnum 结果枚举
     * @return 错误消息
     */
    public static <T> R<T> fail(ResultCodeEnum resultCodeEnum) {
        return new R<>(resultCodeEnum.getCode(), resultCodeEnum.getMessage());
    }

    /**
     * 返回错误消息
     *
     * @param resultCodeEnum 结果枚举
     * @param result 数据对象
     * @return 错误消息
     */
    public static <T> R<T> fail(ResultCodeEnum resultCodeEnum, T result) {
        return new R<>(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), result);
    }

    /**
     * 返回未授权消息
     *
     * @return 未授权消息
     */
    public static <T> R<T> unauthorized() {
        return new R<>(UNAUTHORIZED, "未检测到登录用户");
    }

    /**
     * 返回未授权消息
     *
     * @param message 返回内容
     * @return 未授权消息
     */
    public static <T> R<T> unauthorized(String message) {
        return new R<>(UNAUTHORIZED, message);
    }

    /**
     * 返回禁止访问消息
     *
     * @return 禁止访问消息
     */
    public static <T> R<T> forbidden() {
        return new R<>(FORBIDDEN, "没有权限访问该资源");
    }

    /**
     * 返回禁止访问消息
     *
     * @param message 返回内容
     * @return 禁止访问消息
     */
    public static <T> R<T> forbidden(String message) {
        return new R<>(FORBIDDEN, message);
    }

    /**
     * 返回资源未找到消息
     *
     * @return 资源未找到消息
     */
    public static <T> R<T> notFound() {
        return new R<>(NOT_FOUND, "请求的资源不存在");
    }

    /**
     * 返回资源未找到消息
     *
     * @param message 返回内容
     * @return 资源未找到消息
     */
    public static <T> R<T> notFound(String message) {
        return new R<>(NOT_FOUND, message);
    }

    /**
     * 返回参数错误消息
     *
     * @return 参数错误消息
     */
    public static <T> R<T> badRequest() {
        return new R<>(BAD_REQUEST, "请求参数错误");
    }

    /**
     * 返回参数错误消息
     *
     * @param message 返回内容
     * @return 参数错误消息
     */
    public static <T> R<T> badRequest(String message) {
        return new R<>(BAD_REQUEST, message);
    }

    /**
     * 判断是否成功
     *
     * @return true 成功 false 失败
     */
    public boolean isSuccess() {
        return SUCCESS == code;
    }

    /**
     * 判断是否失败
     *
     * @return true 失败 false 成功
     */
    public boolean isFail() {
        return !isSuccess();
    }
}