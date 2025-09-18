
package com.iceeboot.common.service.rate.service;

import com.iceeboot.common.enums.RateLimitType;

/**
 * 限流服务接口
 *
 * @author CodeIcee
 * @date 2025-09-18
 */
public interface RateLimitService {
    
    /**
     * 检查是否允许访问
     * 
     * @param key 限流key
     * @param count 限制次数
     * @param time 时间窗口（秒）
     * @param limitType 限流类型
     * @return 是否允许访问
     */
    boolean isAllowed(String key, int count, int time, RateLimitType limitType);
    
    /**
     * 获取当前访问次数
     * 
     * @param key 限流key
     * @param limitType 限流类型
     * @return 当前访问次数
     */
    int getCurrentCount(String key, RateLimitType limitType);
    
    /**
     * 获取剩余访问次数
     * 
     * @param key 限流key
     * @param count 限制次数
     * @param time 时间窗口（秒）
     * @param limitType 限流类型
     * @return 剩余访问次数
     */
    int getRemainingCount(String key, int count, int time, RateLimitType limitType);
    
    /**
     * 获取重置时间（秒）
     * 
     * @param key 限流key
     * @param limitType 限流类型
     * @return 重置时间（秒）
     */
    long getResetTime(String key, RateLimitType limitType);
    
    /**
     * 清除限流记录
     * 
     * @param key 限流key
     * @param limitType 限流类型
     */
    void clearLimit(String key, RateLimitType limitType);
}