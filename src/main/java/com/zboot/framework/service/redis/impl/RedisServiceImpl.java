package com.zboot.framework.service.redis.impl;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.utils.RedisUtils;
import com.zboot.framework.query.redis.RedisQuery;
import com.zboot.framework.service.redis.RedisService;
import com.zboot.framework.vo.redis.RedisKeyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis管理服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisUtils redisUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageResult<RedisKeyVO> getRedisKeys(RedisQuery query) {
        // 获取所有键
        Set<String> keys = redisTemplate.keys("*");
        
        // 过滤键名
        if (StringUtils.hasText(query.getKey())) {
            keys = keys.stream()
                    .filter(key -> key.contains(query.getKey()))
                    .collect(Collectors.toSet());
        }
        
        // 过滤类型
        if (StringUtils.hasText(query.getType())) {
            keys = keys.stream()
                    .filter(key -> {
                        String type = redisTemplate.type(key).code();
                        return query.getType().equals(type);
                    })
                    .collect(Collectors.toSet());
        }
        
        // 分页处理
        List<String> keyList = keys.stream().collect(Collectors.toList());
        int total = keyList.size();
        int start = (query.getPageNum() - 1) * query.getPageSize();
        int end = Math.min(start + query.getPageSize(), total);
        
        List<String> pageKeys = keyList.subList(start, end);
        
        // 转换为VO
        List<RedisKeyVO> redisKeyVOList = pageKeys.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return PageResult.of(redisKeyVOList, (long) total, (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public void deleteRedisKey(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void deleteRedisKeys(List<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public RedisKeyVO getRedisKeyDetail(String key) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return null;
        }
        return convertToVO(key);
    }

    /**
     * 转换为VO对象
     */
    private RedisKeyVO convertToVO(String key) {
        RedisKeyVO vo = new RedisKeyVO();
        vo.setKey(key);
        
        // 获取类型
        String type = redisTemplate.type(key).code();
        vo.setType(type);
        
        // 根据类型获取值
        Object value = getValueByType(key, type);
        vo.setValue(value);
        
        // 获取TTL
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        vo.setTtl(expire);
        
        // 计算大小（估算）
        String valueStr = value != null ? value.toString() : "";
        vo.setSize(String.valueOf(valueStr.getBytes().length));
        
        return vo;
    }
    
    /**
     * 根据类型获取值
     */
    private Object getValueByType(String key, String type) {
        try {
            switch (type.toLowerCase()) {
                case "string":
                    return redisTemplate.opsForValue().get(key);
                case "hash":
                    return redisTemplate.opsForHash().entries(key);
                case "list":
                    return redisTemplate.opsForList().range(key, 0, -1);
                case "set":
                    return redisTemplate.opsForSet().members(key);
                case "zset":
                    return redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
                default:
                    return redisTemplate.opsForValue().get(key);
            }
        } catch (Exception e) {
            return "Error reading value: " + e.getMessage();
        }
    }
}