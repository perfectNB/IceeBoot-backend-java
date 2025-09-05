package com.zboot.framework.service.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.dto.create.ai.AiChatSessionDTO;
import com.zboot.framework.dto.create.ai.AiChatMessageDTO;
import com.zboot.framework.entity.ai.AiChatSessionDO;
import com.zboot.framework.query.ai.AiChatSessionQuery;
import com.zboot.framework.vo.ai.AiChatSessionVO;
import com.zboot.framework.vo.ai.AiChatMessageVO;

import java.util.List;

/**
 * AI对话服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface IAiChatService extends IService<AiChatSessionDO> {

    /**
     * 分页查询会话列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<AiChatSessionVO> getSessionPage(AiChatSessionQuery query);

    /**
     * 根据ID获取会话详情
     *
     * @param id 会话ID
     * @return 会话详情
     */
    AiChatSessionVO getSessionById(Long id);

    /**
     * 新增会话
     *
     * @param dto 会话信息
     * @return 是否成功
     */
    Boolean addSession(AiChatSessionDTO dto);

    /**
     * 更新会话
     * 
     * @param dto 会话信息
     * @return 是否成功
     */
    Boolean updateSession(AiChatSessionDTO dto);

    /**
     * 删除会话
     *
     * @param ids 会话ID列表
     * @return 是否成功
     */
    Boolean deleteSession(List<Long> ids);

    /**
     * 根据用户ID获取会话列表
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    List<AiChatSessionVO> getSessionsByUserId(Long userId);

    /**
     * 发送消息
     * 
     * @param dto 消息信息
     * @return 消息详情
     */
    AiChatMessageVO sendMessage(AiChatMessageDTO dto);

    /**
     * 获取会话消息列表
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<AiChatMessageVO> getSessionMessages(Long sessionId);

    /**
     * 清空会话消息
     * 
     * @param sessionId 会话ID
     * @return 是否成功
     */
    Boolean clearSessionMessages(Long sessionId);
}