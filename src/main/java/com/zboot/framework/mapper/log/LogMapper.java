/**
 * 日志Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.mapper.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.log.LogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日志Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface LogMapper extends BaseMapper<LogDO> {

}