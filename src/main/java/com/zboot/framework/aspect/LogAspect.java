/**
 * 日志记录切面
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.aspect;

import com.zboot.framework.config.LogConfig;
import com.zboot.framework.service.log.LogService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 日志记录切面
 * 用于拦截Controller方法并记录日志
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final LogService logService;
    private final LogConfig logConfig;

    /**
     * 定义切点：拦截所有Controller包下的方法
     */
    @Pointcut("execution(* com.zboot.framework.controller..*(..))") 
    public void controllerPointcut() {}

    /**
     * 方法执行后记录日志
     */
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        try {
            // 检查是否启用日志记录
            if (!logConfig.isEnabled()) {
                return;
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String requestPath = request.getRequestURI();
            String httpMethod = request.getMethod();
            
            // 获取方法信息
            Method method = getMethod(joinPoint);
            if (method == null) {
                return;
            }
            
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = method.getName();
            
            // 获取Operation注解信息
            String summary = "";
            Operation operation = method.getAnnotation(Operation.class);
            if (operation != null) {
                summary = operation.summary();
            }
            
            // 构建日志消息
            String message = String.format("执行%s请求: %s, 方法: %s.%s", 
                httpMethod, requestPath, className, methodName);
            
            // 根据配置的日志级别记录日志
            String logLevel = logConfig.getLevel().toUpperCase();
            
            // 记录到slf4j日志
            switch (logLevel) {
                case "DEBUG":
                    log.debug(message);
                    break;
                case "INFO":
                    log.info(message);
                    break;
                case "WARN":
                    log.warn(message);
                    break;
                case "ERROR":
                    log.error(message);
                    break;
                default:
                    log.info(message);
                    break;
            }
            
            // 异步保存到数据库
            logService.saveLogAsync(
                logLevel,
                getModuleName(className),
                message,
                joinPoint.getTarget().getClass().getName(),
                summary,
                requestPath
            );
            
        } catch (Exception e) {
            log.error("记录日志时发生异常", e);
        }
    }
    
    /**
     * 获取方法对象
     */
    private Method getMethod(JoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            Class<?>[] parameterTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    parameterTypes[i] = args[i].getClass();
                } else {
                    parameterTypes[i] = Object.class;
                }
            }
            
            Class<?> targetClass = joinPoint.getTarget().getClass();
            Method[] methods = targetClass.getDeclaredMethods();
            
            // 查找匹配的方法
            for (Method method : methods) {
                if (method.getName().equals(methodName) && 
                    method.getParameterCount() == parameterTypes.length) {
                    return method;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取方法信息失败", e);
            return null;
        }
    }
    
    /**
     * 根据类名获取模块名
     */
    private String getModuleName(String className) {
        if (className.contains("System") || className.contains("User") || className.contains("Role")) {
            return "系统管理";
        } else if (className.contains("Redis")) {
            return "Redis管理";
        } else if (className.contains("Log")) {
            return "日志管理";
        } else if (className.contains("Dashboard")) {
            return "仪表盘";
        } else if (className.contains("Task")) {
            return "任务管理";
        } else if (className.contains("Workflow")) {
            return "工作流";
        } else if (className.contains("Ai")) {
            return "AI模块";
        } else {
            return "其他";
        }
    }
}