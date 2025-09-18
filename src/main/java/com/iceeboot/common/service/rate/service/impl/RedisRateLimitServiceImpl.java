package com.iceeboot.common.service.rate.service.impl;

import com.iceeboot.common.enums.RateLimitType;
import com.iceeboot.common.service.rate.service.RateLimitService;
import com.iceeboot.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
/**
 * 基于Redis的限流服务实现
 * 使用滑动窗口算法实现精确限流
 *
 * @author CodeIcee
 * @date 2025-09-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimitServiceImpl implements RateLimitService {
    
    private final RedisUtils redisUtils;
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String COUNT_SUFFIX = ":count";
    private static final String WINDOW_SUFFIX = ":window";
    
    @Override
    public boolean isAllowed(String key, int count, int time, RateLimitType limitType) {
        String redisKey = buildRedisKey(key, limitType);
        
        try {
            // 使用简化的计数器算法（由于RedisUtils没有Lua脚本支持）
            String countKey = redisKey + COUNT_SUFFIX;
            String windowKey = redisKey + WINDOW_SUFFIX;
            
            // 获取当前时间窗口开始时间
            Object windowStartObj = redisUtils.get(windowKey);
            long currentTime = System.currentTimeMillis();
            long windowStart = windowStartObj != null ? Long.parseLong(windowStartObj.toString()) : 0;
            
            // 检查时间窗口是否过期
            if (windowStart == 0 || (currentTime - windowStart) >= (time * 1000L)) {
                // 重置计数器和时间窗口
                redisUtils.set(countKey, 1, time);
                redisUtils.set(windowKey, currentTime, time);
                return true;
            }
            
            // 获取当前计数
            Object currentCountObj = redisUtils.get(countKey);
            int currentCount = currentCountObj != null ? Integer.parseInt(currentCountObj.toString()) : 0;
            
            // 检查是否超过限制
            boolean allowed = currentCount < count;
            if (allowed) {
                redisUtils.incr(countKey, 1);
            }
            
            if (!allowed) {
                log.warn("超出密钥的速率限制: {}, type: {}, limit: {}/{} seconds",
                    key, limitType, count, time);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("密钥的速率限制检查失败: {}, error: {}", key, e.getMessage(), e);
            // 发生异常时默认允许访问，避免影响正常业务
            return true;
        }
    }
    
    @Override
    public int getCurrentCount(String key, RateLimitType limitType) {
        String redisKey = buildRedisKey(key, limitType) + COUNT_SUFFIX;
        
        try {
            Object count = redisUtils.get(redisKey);
            return count != null ? Integer.parseInt(count.toString()) : 0;
        } catch (Exception e) {
            log.error("获取密钥的当前计数失败: {}, error: {}", key, e.getMessage());
            return 0;
        }
    }
    
    @Override
    public int getRemainingCount(String key, int count, int time, RateLimitType limitType) {
        int currentCount = getCurrentCount(key, limitType);
        return Math.max(0, count - currentCount);
    }
    
    @Override
    public long getResetTime(String key, RateLimitType limitType) {
        String redisKey = buildRedisKey(key, limitType) + WINDOW_SUFFIX;
        
        try {
            Object windowStart = redisUtils.get(redisKey);
            if (windowStart != null) {
                long startTime = Long.parseLong(windowStart.toString());
                long ttl = redisUtils.getExpire(redisKey);
                return ttl > 0 ? ttl : 0;
            }
            return 0;
        } catch (Exception e) {
            log.error("获取密钥重置时间失败: {}, error: {}", key, e.getMessage());
            return 0;
        }
    }
    
    @Override
    public void clearLimit(String key, RateLimitType limitType) {
        String redisKey = buildRedisKey(key, limitType);
        
        try {
            redisUtils.del(redisKey + COUNT_SUFFIX);
            redisUtils.del(redisKey + WINDOW_SUFFIX);
            log.info("已清除密钥的速率限制: {}, type: {}", key, limitType);
        } catch (Exception e) {
            log.error("密钥清除速率限制失败: {}, error: {}", key, e.getMessage());
        }
    }
    
    /**
     * 构建Redis key
     */
    private String buildRedisKey(String key, RateLimitType limitType) {
        return RATE_LIMIT_PREFIX + limitType.getCode() + ":" + key;
    }
    

}