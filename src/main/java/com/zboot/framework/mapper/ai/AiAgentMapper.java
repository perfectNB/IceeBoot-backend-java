/*
 * @Description: AI代理Mapper接口
 * @Date: 2025-01-27
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
 * @since 2025-01-27
 */
@Mapper
public interface AiAgentMapper extends BaseMapper<AiAgentDO> {

    /**
     * 根据平台获取代理列表
     *
     * @param platform 平台
     * @return 代理列表
     */
    List<AiAgentDO> selectByPlatform(@Param("platform") String platform);

    /**
     * 根据类型获取代理列表
     *
     * @param type 类型
     * @return 代理列表
     */
    List<AiAgentDO> selectByType(@Param("type") String type);

    /**
     * 根据平台和类型获取代理列表
     *
     * @param platform 平台
     * @param type 类型
     * @return 代理列表
     */
    List<AiAgentDO> selectByPlatformAndType(@Param("platform") String platform, @Param("type") String type);

    /**
     * 根据状态获取代理列表
     *
     * @param status 状态
     * @return 代理列表
     */
    List<AiAgentDO> selectByStatus(@Param("status") String status);
}