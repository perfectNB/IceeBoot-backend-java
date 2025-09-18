package com.iceeboot.common.annotation;

import com.iceeboot.common.enums.RateLimitType;

import java.lang.annotation.*;

/**
 * 限流注解
 * 可以应用在Controller方法上，实现接口访问频率限制
 * 
 * 使用示例：
 * @RateLimit(count = 10, time = 60, limitType = RateLimitType.USER)
 * @RateLimit(count = 100, time = 60, limitType = RateLimitType.GLOBAL)
 * @author CodeIcee
 * @date 2025-09-16
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流key的前缀
     * 默认为方法名
     */
    String key() default "";
    
    /**
     * 限流时间窗口，单位：秒
     * 默认60秒
     */
    int time() default 60;
    
    /**
     * 限流次数
     * 在time时间窗口内允许的最大访问次数
     * 默认100次
     */
    int count() default 100;
    
    /**
     * 限流类型
     * USER: 按用户限流
     * GLOBAL: 全局限流
     * IP: 按IP限流
     */
    RateLimitType limitType() default RateLimitType.USER;
    
    /**
     * 限流失败时的提示信息
     */
    String message() default "访问过于频繁，请稍后再试";
    
    /**
     * 是否启用限流
     * 可以通过配置动态控制
     */
    boolean enabled() default true;
}