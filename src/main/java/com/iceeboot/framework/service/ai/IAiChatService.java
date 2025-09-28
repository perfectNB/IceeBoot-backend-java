package com.iceeboot.framework.service.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.framework.dto.create.ai.AiChatSessionDTO;
import com.iceeboot.framework.dto.create.ai.AiChatMessageDTO;
import com.iceeboot.framework.dto.create.ai.StreamChatRequest;
import com.iceeboot.framework.entity.ai.AiChatSessionDO;
import com.iceeboot.framework.query.ai.AiChatSessionQuery;
import com.iceeboot.framework.query.ai.AiChatMessageQuery;
import com.iceeboot.framework.vo.ai.AiChatSessionVO;
import com.iceeboot.framework.vo.ai.AiChatMessageVO;
import reactor.core.publisher.Flux;

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
    
    /**
     * 获取会话消息（分页）
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<AiChatMessageVO> getMessage(AiChatMessageQuery query);

    /**
     * 流式对话
     *
     * @param request 流式对话请求
     * @return 流式响应
     */
    Flux<LLMResponse> streamChat(StreamChatRequest request);

    /**
     * 流式对话
     *
     * @param request 工具调用流式对话
     * @return 流式响应
     */
    Flux<String> toolStreamChat(StreamChatRequest request);
}