/**
 * Redis Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.redis;

import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.redis.CreateRedisKeyDTO;
import com.zboot.framework.query.redis.RedisQuery;
import com.zboot.framework.vo.redis.RedisKeyVO;

import java.util.List;

/**
 * Redis Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface RedisService {

    /**
     * 获取Redis键列表
     * 
     * @param query 查询条件
     * @return Redis键列表
     */
    PageResult<RedisKeyVO> getRedisKeys(RedisQuery query);

    /**
     * 删除Redis键
     * 
     * @param key Redis键名
     */
    void deleteRedisKey(String key);

    /**
     * 批量删除Redis键
     * 
     * @param keys Redis键名列表
     */
    void deleteRedisKeys(List<String> keys);

    /**
     * 创建Redis键
     * 
     * @param createDTO 创建Redis键DTO
     */
    void createRedisKey(CreateRedisKeyDTO createDTO);

    /**
     * 获取Redis键详情
     * 
     * @param key Redis键名
     * @return Redis键详情
     */
    RedisKeyVO getRedisKeyDetail(String key);
}