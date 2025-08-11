/**
 * Redis Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.mapper.redis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.redis.RedisDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Redis Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface RedisMapper extends BaseMapper<RedisDO> {

}