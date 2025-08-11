package com.zboot.framework.service.impl.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.framework.dto.ai.AiChatSessionDTO;
import com.zboot.framework.dto.ai.AiChatMessageDTO;
import com.zboot.framework.entity.ai.AiChatSessionDO;
import com.zboot.framework.entity.ai.AiChatMessageDO;
import com.zboot.framework.mapper.ai.AiChatSessionMapper;
import com.zboot.framework.mapper.ai.AiChatMessageMapper;
import com.zboot.framework.query.ai.AiChatSessionQuery;
import com.zboot.framework.service.ai.IAiChatService;
import com.zboot.framework.vo.ai.AiChatSessionVO;
import com.zboot.framework.vo.ai.AiChatMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI对话服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSessionDO> implements IAiChatService {

    private final AiChatMessageMapper aiChatMessageMapper;

    @Override
    public IPage<AiChatSessionVO> getSessionPage(AiChatSessionQuery query) {
        Page<AiChatSessionDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AiChatSessionDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(AiChatSessionDO::getTitle, query.getKeyword())
                   .or()
                   .like(AiChatSessionDO::getModelName, query.getKeyword());
        }
        if (query.getUserId() != null) {
            wrapper.eq(AiChatSessionDO::getUserId, query.getUserId());
        }
        if (query.getModelId() != null) {
            wrapper.eq(AiChatSessionDO::getModelId, query.getModelId());
        }
        
        wrapper.orderByDesc(AiChatSessionDO::getCreateTime);
        
        IPage<AiChatSessionDO> result = this.page(page, wrapper);
        
        // 转换为VO
        Page<AiChatSessionVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<AiChatSessionVO> voList = result.getRecords().stream().map(this::convertSessionToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    @Override
    public AiChatSessionVO getSessionById(Long id) {
        AiChatSessionDO session = this.getById(id);
        if (session == null) {
            return null;
        }
        
        AiChatSessionVO vo = convertSessionToVO(session);
        
        // 获取会话消息列表
        List<AiChatMessageVO> messages = getSessionMessages(id);
        vo.setMessages(messages);
        
        return vo;
    }

    @Override
    public Boolean addSession(AiChatSessionDTO dto) {
        AiChatSessionDO session = new AiChatSessionDO();
        BeanUtils.copyProperties(dto, session);
        return this.save(session);
    }

    @Override
    public Boolean updateSession(AiChatSessionDTO dto) {
        AiChatSessionDO session = new AiChatSessionDO();
        BeanUtils.copyProperties(dto, session);
        return this.updateById(session);
    }

    @Override
    public Boolean deleteSession(List<Long> ids) {
        // 删除会话时同时删除相关消息
        for (Long id : ids) {
            clearSessionMessages(id);
        }
        return this.removeByIds(ids);
    }

    @Override
    public List<AiChatSessionVO> getSessionsByUserId(Long userId) {
        QueryWrapper<AiChatSessionDO> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("update_time");
        
        List<AiChatSessionDO> sessions = this.list(wrapper);
        return sessions.stream().map(this::convertSessionToVO).collect(Collectors.toList());
    }

    @Override
    public AiChatMessageVO sendMessage(AiChatMessageDTO dto) {
        AiChatMessageDO message = new AiChatMessageDO();
        BeanUtils.copyProperties(dto, message);
        message.setTimestamp(LocalDateTime.now());
        
        aiChatMessageMapper.insert(message);
        
        return convertMessageToVO(message);
    }

    @Override
    public List<AiChatMessageVO> getSessionMessages(Long sessionId) {
        QueryWrapper<AiChatMessageDO> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
               .orderByAsc("timestamp");
        
        List<AiChatMessageDO> messages = aiChatMessageMapper.selectList(wrapper);
        return messages.stream().map(this::convertMessageToVO).collect(Collectors.toList());
    }

    @Override
    public Boolean clearSessionMessages(Long sessionId) {
        QueryWrapper<AiChatMessageDO> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        
        return aiChatMessageMapper.delete(wrapper) >= 0;
    }

    /**
     * 转换会话为VO对象
     */
    private AiChatSessionVO convertSessionToVO(AiChatSessionDO session) {
        AiChatSessionVO vo = new AiChatSessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    /**
     * 转换消息为VO对象
     */
    private AiChatMessageVO convertMessageToVO(AiChatMessageDO message) {
        AiChatMessageVO vo = new AiChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}