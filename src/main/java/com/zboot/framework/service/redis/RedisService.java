/**
 * Redis Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.redis;

import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.redis.RedisQueryDTO;
import com.zboot.framework.vo.redis.RedisKeyVO;

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
     * @param queryDTO 查询条件
     * @return Redis键列表
     */
    PageResult<RedisKeyVO> getRedisKeys(RedisQueryDTO queryDTO);

    /**
     * 删除Redis键
     * 
     * @param key Redis键名
     */
    void deleteRedisKey(String key);

    /**
     * 获取Redis键详情
     * 
     * @param key Redis键名
     * @return Redis键详情
     */
    RedisKeyVO getRedisKeyDetail(String key);
}