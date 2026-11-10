/*
 * @Description: 全局异常处理器
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.iceeboot.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.service.ai.agent.exception.AgentException;
import com.iceeboot.common.service.oss.exception.OSSException;
import com.iceeboot.common.service.sms.exception.SMSException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * @author CodeIcee
 * @since 2025-08-11
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String EVENT_STREAM = "text/event-stream";

    /**
     * 权限码异常
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限码校验失败'{}'", requestURI, e.getMessage());
        return R.forbidden(ResponseMessage.Common.FORBIDDEN);
    }

    /**
     * 角色权限异常
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',角色权限校验失败'{}'", requestURI, e.getMessage());
        return R.forbidden(ResponseMessage.Common.FORBIDDEN);
    }

    /**
     * 认证失败
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',认证失败'{}',无法访问系统资源", requestURI, e.getMessage());
        return R.unauthorized(ResponseMessage.Common.UNAUTHORIZED);
    }

    /** Authentication storage failures are retryable service errors, never invalid credentials. */
    @ExceptionHandler(AuthServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public R<Void> handleAuthServiceUnavailable(AuthServiceUnavailableException e, HttpServletRequest request) {
        Throwable cause = e.getCause();
        log.error("请求地址'{}',登录验证服务暂不可用, errorType={}", request.getRequestURI(),
                cause == null ? "unknown" : cause.getClass().getSimpleName());
        return R.fail(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getMessage());
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                        HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return R.fail(ResponseMessage.Common.METHOD_NOT_ALLOWED);
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return code != null ? R.fail(code, e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * 限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',触发限流: {}, 限制: {}/{} 秒, 当前: {} 次", 
            requestURI, e.getMessage(), e.getLimitCount(), e.getTimeWindow(), e.getCurrentCount());
        String message = e.getMessage();
        return R.fail(message == null || message.isBlank() ? ResponseMessage.Common.RATE_LIMIT : message);
    }

    /**
     * 数据权限异常
     */
    @ExceptionHandler(DataPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleDataPermissionException(DataPermissionException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',数据权限校验失败: {}", requestURI, e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * OSS异常
     */
    @ExceptionHandler(OSSException.class)
    public R<Void> handleOSSException(OSSException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',OSS操作异常: {}", requestURI, e.getDetailedMessage(), e);
        String code = e.getErrorCode();
        return code != null ? R.fail(code + ": " + e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * SMS异常
     */
    @ExceptionHandler(SMSException.class)
    public R<Void> handleSMSException(SMSException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',SMS操作异常: {}", requestURI, e.getDetailedMessage(), e);
        String code = e.getErrorCode();
        return code != null ? R.fail(code + ": " + e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * Agent异常
     */
    @ExceptionHandler(AgentException.class)
    public R<Void> handleAgentException(AgentException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',Agent操作异常: [{}] [{}] {}", requestURI, e.getProvider(), e.getErrorCode(), e.getMessage(), e);
        String code = e.getErrorCode();
        return code != null ? R.fail(code + ": " + e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * iceeboot自定义异常
     */
    @ExceptionHandler(IceeBootException.class)
    public R<Void> handleiceebootException(IceeBootException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生自定义异常: {}", requestURI, e.getMessage());
        Integer code = e.getCode();
        return code != null ? R.fail(code, e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * SSE/异步响应在客户端断开连接时会抛出该异常。
     *
     * <p>这是连接生命周期事件，不应再尝试写 JSON 错误体，
     * 否则会在 text/event-stream 响应上再次触发 HttpMessageNotWritableException。</p>
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (isSseRequest(request) || isClientDisconnect(e)) {
            log.warn("请求地址'{}',客户端已断开{}连接: {}", requestURI, isSseRequest(request) ? "SSE" : "HTTP", rootMessage(e));
            return;
        }
        log.error("请求地址'{}',异步请求不可用", requestURI, e);
    }

    /**
     * 如果前面的异常处理器在 SSE 响应上误写 JSON，会触发这个异常。
     *
     * <p>这里兜底吞掉 event-stream 场景，避免日志被二次异常淹没。</p>
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public Object handleHttpMessageNotWritableException(HttpMessageNotWritableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (isSseRequest(request)) {
            log.warn("请求地址'{}',SSE响应已提交，跳过JSON错误回写: {}", requestURI, rootMessage(e));
            return null;
        }
        log.error("请求地址'{}',响应写出失败", requestURI, e);
        return R.fail(ResponseMessage.Common.SYSTEM_ERROR);
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',参数校验失败: {}", requestURI, e.getMessage());
        return R.fail(e.getMessage());
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return R.fail(ResponseMessage.Common.SYSTEM_ERROR);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return R.fail(ResponseMessage.Common.SYSTEM_ERROR);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining(", "));
        return R.badRequest(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.error(e.getMessage(), e);
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return R.badRequest(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.badRequest(message);
    }

    /**
     * 404异常处理
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',无法找到该资源.", requestURI, e);
        return R.notFound(ResponseMessage.Common.NOT_FOUND);
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(EVENT_STREAM)) {
            return true;
        }
        String contentType = request.getContentType();
        return contentType != null && contentType.contains(EVENT_STREAM);
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset by peer")
                        || normalized.contains("disconnected client")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = null;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message == null ? "" : message;
    }
}
