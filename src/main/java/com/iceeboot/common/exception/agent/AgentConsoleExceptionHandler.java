package com.iceeboot.common.exception.agent;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.exception.IceeBootException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/** 智能体接口异常处理。框架业务异常返回常量消息，第三方异常不暴露原始消息或凭据。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.iceeboot.framework.controller.agent")
public class AgentConsoleExceptionHandler {
    @ExceptionHandler(IceeBootException.class)
    public ResponseEntity<R<Void>> business(IceeBootException e) {
        int code = e.getCode() == null ? HttpStatus.BAD_REQUEST.value() : e.getCode();
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null || !status.isError()) status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(R.fail(code, e.getMessage()));
    }

    @ExceptionHandler({
        SecurityException.class,
        cn.dev33.satoken.exception.NotPermissionException.class
    })
    public ResponseEntity<R<Void>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.forbidden(ResponseMessage.Agent.PERMISSION_DENIED));
    }

    @ExceptionHandler(cn.dev33.satoken.exception.NotLoginException.class)
    public ResponseEntity<R<Void>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.unauthorized(ResponseMessage.Agent.LOGIN_REQUIRED));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        R.fail(
                                HttpStatus.CONFLICT.value(),
                                ResponseMessage.Agent.IDENTITY_OR_VERSION_EXISTS));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<R<Void>> validation() {
        return ResponseEntity.badRequest()
                .body(R.badRequest(ResponseMessage.Agent.REQUEST_INVALID));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<R<Void>> failed() {
        return ResponseEntity.internalServerError()
                .body(R.fail(ResponseMessage.Agent.OPERATION_FAILED));
    }
}
