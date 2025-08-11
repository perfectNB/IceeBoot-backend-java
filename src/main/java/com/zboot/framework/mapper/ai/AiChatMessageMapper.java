/*
 * @Description: AI对话消息Mapper接口
 * @Date: 2025-01-27
 * @Author: CodeIcee
 */
package com.zboot.framework.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.ai.AiChatMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI对话消息Mapper接口
 *
 * @author CodeIcee
 * @since 2025-01-27
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessageDO> {

    /**
     * 根据会话ID获取消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<AiChatMessageDO> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据用户ID获取消息列表
     *
     * @param userId 用户ID
     * @return 消息列表
     */
    List<AiChatMessageDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据角色获取消息列表
     *
     * @param role 角色
     * @return 消息列表
     */
    List<AiChatMessageDO> selectByRole(@Param("role") String role);

    /**
     * 根据会话ID和角色获取消息列表
     *
     * @param sessionId 会话ID
     * @param role 角色
     * @return 消息列表
     */
    List<AiChatMessageDO> selectBySessionIdAndRole(@Param("sessionId") Long sessionId, @Param("role") String role);

    /**
     * 根据会话ID删除消息
     *
     * @param sessionId 会话ID
     * @return 删除数量
     */
    int deleteBySessionId(@Param("sessionId") Long sessionId);
}