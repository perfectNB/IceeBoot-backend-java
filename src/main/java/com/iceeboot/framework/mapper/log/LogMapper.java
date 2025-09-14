/**
 * 日志Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.mapper.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.log.LogDO;
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