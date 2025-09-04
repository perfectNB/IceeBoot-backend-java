/*
 * @Description: AI代理Mapper接口
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.zboot.framework.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.ai.AiAgentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI代理Mapper接口
 *
 * @author CodeIcee
 * @since 2025-08-11
 */
@Mapper
public interface AiAgentMapper extends BaseMapper<AiAgentDO> {


}