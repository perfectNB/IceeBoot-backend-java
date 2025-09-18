package com.iceeboot.common.aspect;

import com.iceeboot.common.annotation.RateLimit;
import com.iceeboot.common.exception.RateLimitException;
import com.iceeboot.common.service.rate.service.RateLimitService;
import com.iceeboot.common.utils.IpUtils;
import com.iceeboot.common.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 限流切面处理器
 * 拦截带有@RateLimit注解的方法，执行限流逻辑
 *
 * @author CodeIcee
 * @date 2025-09-18
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RateLimitService rateLimitService;
    
    /**
     * 拦截带有@RateLimit注解的方法
     */
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) throws Throwable {
        // 检查是否启用限流
        if (!rateLimit.enabled()) {
            return;
        }
        
        // 获取限流key
        String limitKey = getLimitKey(point, rateLimit);
        
        // 执行限流检查
        boolean allowed = rateLimitService.isAllowed(
            limitKey, 
            rateLimit.count(), 
            rateLimit.time(), 
            rateLimit.limitType()
        );
        
        if (!allowed) {
            // 获取当前访问次数用于异常信息
            int currentCount = rateLimitService.getCurrentCount(limitKey, rateLimit.limitType());
            
            log.warn("Rate limit exceeded: key={}, type={}, limit={}/{} seconds, current={}", 
                limitKey, rateLimit.limitType(), rateLimit.count(), rateLimit.time(), currentCount);
            
            throw new RateLimitException(
                rateLimit.message(), 
                limitKey, 
                rateLimit.time(), 
                rateLimit.count(), 
                currentCount
            );
        }
        
        log.debug("Rate limit check passed: key={}, type={}, limit={}/{} seconds", 
            limitKey, rateLimit.limitType(), rateLimit.count(), rateLimit.time());
    }
    
    /**
     * 获取限流key
     */
    private String getLimitKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 基础key：注解指定的key或方法名
        String baseKey = rateLimit.key();
        if (baseKey.isEmpty()) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            baseKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
        keyBuilder.append(baseKey);
        
        // 根据限流类型添加后缀
        switch (rateLimit.limitType()) {
            case USER:
                // 按用户限流：添加用户ID
                String userId = getCurrentUserId();
                if (userId != null) {
                    keyBuilder.append(":").append(userId);
                } else {
                    // 如果获取不到用户ID，降级为IP限流
                    keyBuilder.append(":").append(getCurrentIp());
                }
                break;
                
            case IP:
                // 按IP限流：添加IP地址
                keyBuilder.append(":").append(getCurrentIp());
                break;
                
            case GLOBAL:
                // 全局限流：不添加后缀
                break;
                
            default:
                break;
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            // 从Token中获取用户ID
            Long userId = TokenUtils.getCurrentUserId();
            return userId != null ? userId.toString() : null;
        } catch (Exception e) {
            log.debug("Failed to get user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取当前请求的IP地址
     */
    private String getCurrentIp() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return IpUtils.getClientIp(request);
            }
        } catch (Exception e) {
            log.debug("Failed to get IP address: {}", e.getMessage());
        }
        return "unknown";
    }
}