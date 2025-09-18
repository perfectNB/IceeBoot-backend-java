package com.iceeboot.common.exception;

/**
 * 限流异常
 * 当访问频率超过限制时抛出此异常
 * @author CodeIcee
 * @date 2025-09-18
 */
public class RateLimitException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 限流key
     */
    private String limitKey;
    
    /**
     * 限流时间窗口（秒）
     */
    private int timeWindow;
    
    /**
     * 限流次数
     */
    private int limitCount;
    
    /**
     * 当前访问次数
     */
    private int currentCount;
    
    public RateLimitException(String message) {
        super(message);
    }
    
    public RateLimitException(String message, String limitKey, int timeWindow, int limitCount, int currentCount) {
        super(message);
        this.limitKey = limitKey;
        this.timeWindow = timeWindow;
        this.limitCount = limitCount;
        this.currentCount = currentCount;
    }
    
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getLimitKey() {
        return limitKey;
    }
    
    public void setLimitKey(String limitKey) {
        this.limitKey = limitKey;
    }
    
    public int getTimeWindow() {
        return timeWindow;
    }
    
    public void setTimeWindow(int timeWindow) {
        this.timeWindow = timeWindow;
    }
    
    public int getLimitCount() {
        return limitCount;
    }
    
    public void setLimitCount(int limitCount) {
        this.limitCount = limitCount;
    }
    
    public int getCurrentCount() {
        return currentCount;
    }
    
    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }
}