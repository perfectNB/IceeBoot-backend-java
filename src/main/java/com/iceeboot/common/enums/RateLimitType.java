package com.iceeboot.common.enums;

/**
 * 限流类型枚举
 *
 * @author CodeIcee
 * @date 2025-09-18
 */
public enum RateLimitType {
    
    /**
     * 按用户限流
     * 每个用户独立计算访问次数
     */
    USER("user", "按用户限流"),
    
    /**
     * 全局限流
     * 所有用户共享访问次数限制
     */
    GLOBAL("global", "全局限流"),
    
    /**
     * 按IP限流
     * 每个IP地址独立计算访问次数
     */
    IP("ip", "按IP限流");
    
    /**
     * 类型代码
     */
    private final String code;
    
    /**
     * 类型描述
     */
    private final String description;
    
    RateLimitType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}