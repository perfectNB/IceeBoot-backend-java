/*
 * @Description: AI对话会话Mapper接口
 * @Date: 2025-01-27
 * @Author: CodeIcee
 */
package com.zboot.framework.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.ai.AiChatSessionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI对话会话Mapper接口
 *
 * @author CodeIcee
 * @since 2025-01-27
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSessionDO> {

    /**
     * 根据用户ID获取会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<AiChatSessionDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据模型ID获取会话列表
     *
     * @param modelId 模型ID
     * @return 会话列表
     */
    List<AiChatSessionDO> selectByModelId(@Param("modelId") Long modelId);

    /**
     * 根据用户ID和模型ID获取会话列表
     *
     * @param userId 用户ID
     * @param modelId 模型ID
     * @return 会话列表
     */
    List<AiChatSessionDO> selectByUserIdAndModelId(@Param("userId") Long userId, @Param("modelId") Long modelId);

    /**
     * 根据标题模糊查询
     *
     * @param title 会话标题
     * @return 会话列表
     */
    List<AiChatSessionDO> selectByTitleLike(@Param("title") String title);
}