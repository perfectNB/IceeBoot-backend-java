package com.iceeboot.agent.web;

import com.iceeboot.common.core.domain.R;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/** 模型和 MCP 异常可能带第三方凭据，禁止将 SDK 原始异常直接返回控制台。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AgentConsoleController.class)
public class AgentConsoleExceptionHandler {
  @ExceptionHandler({
    SecurityException.class,
    cn.dev33.satoken.exception.NotPermissionException.class
  })
  public ResponseEntity<R<Void>> forbidden() {
    return ResponseEntity.status(403).body(R.fail(403, "没有使用该功能的权限"));
  }

  @ExceptionHandler(cn.dev33.satoken.exception.NotLoginException.class)
  public ResponseEntity<R<Void>> unauthorized() {
    return ResponseEntity.status(401).body(R.fail(401, "请先登录"));
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<R<Void>> duplicate() {
    return ResponseEntity.status(409).body(R.fail(409, "编码或发布版本已存在，请刷新后重试"));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<R<Void>> validation() {
    return ResponseEntity.badRequest().body(R.fail(400, "请检查必填项、编码格式及参数范围"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<R<Void>> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(R.fail(400, e.getMessage()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<R<Void>> failed() {
    return ResponseEntity.internalServerError().body(R.fail(500, "操作失败，请检查模型、能力及运行环境配置"));
  }
}
