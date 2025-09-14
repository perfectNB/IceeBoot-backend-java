/**
 * 消息服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.iceeboot.framework.service.message.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.message.MessageDTO;
import com.iceeboot.framework.entity.message.MessageDO;
import com.iceeboot.framework.mapper.message.IMessageMapper;
import com.iceeboot.framework.query.message.MessageQuery;
import com.iceeboot.framework.service.message.IMessageService;
import com.iceeboot.framework.vo.message.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<IMessageMapper, MessageDO> implements IMessageService {

    private final IMessageMapper messageMapper;

    @Override
    public PageResult<MessageVO> getMessagePage(MessageQuery query) {
        // 获取当前用户ID
        Long userId = TokenUtils.getCurrentUserId();
        if (userId == null) {
            return new PageResult<>(List.of(), 0L, (long) query.getPageNum(), (long) query.getPageSize());
        }

        LambdaQueryWrapper<MessageDO> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询当前用户的消息
        wrapper.eq(MessageDO::getUserId, userId);
        
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(MessageDO::getTitle, query.getTitle());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(MessageDO::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(MessageDO::getType, query.getType());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(MessageDO::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(MessageDO::getCreateTime, query.getEndTime());
        }
        
        wrapper.orderByDesc(MessageDO::getCreateTime);

        Page<MessageDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<MessageDO> pageResult = this.page(page, wrapper);

        List<MessageVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }



    @Override
    @Transactional
    public boolean addMessage(MessageDTO messageDTO) {
        MessageDO message = new MessageDO();
        BeanUtils.copyProperties(messageDTO, message);
        
        // 如果没有设置状态，默认为未读
        if (!StringUtils.hasText(message.getStatus())) {
            message.setStatus(SystemConstants.Status.UNREAD);
        }
        
        return this.save(message);
    }

    @Override
    public boolean allRead() {
        Long userId = TokenUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("allRead failed: userId is null");
            return false;
        }
        // 批量更新所有未读消息为已读
        boolean updated = this.update(
                new LambdaUpdateWrapper<MessageDO>()
                        .eq(MessageDO::getUserId, userId)
                        .eq(MessageDO::getStatus, SystemConstants.Status.UNREAD)
                        .set(MessageDO::getStatus, SystemConstants.Status.READ)
        );

        if (!updated) {
            log.warn("全部已读失败,userId:{}", userId);
        }
        return updated;
    }

    @Override
    public boolean updateMessage(List<Long> ids) {
        for (Long id : ids) {
            MessageDO message = new MessageDO();
            message.setId(id);
            message.setStatus(SystemConstants.Status.READ);
            this.updateById(message);
        }
        return true;
    }


    @Override
    @Transactional
    public boolean deleteMessages(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Map<String, Long> getMessageStats() {
        // 获取当前用户ID
        Long userId = TokenUtils.getCurrentUserId();
        if (userId == null) {
            return Map.of(SystemConstants.Status.UNREAD, 0L, "read", 0L, "total", 0L);
        }

        Map<String, Long> stats = new HashMap<>();
        
        // 统计未读消息数量
        Long unreadCount = messageMapper.countUnreadByUserId(userId);
        stats.put(SystemConstants.Status.UNREAD, unreadCount != null ? unreadCount : 0L);
        
        // 统计已读消息数量
        Long readCount = messageMapper.countReadByUserId(userId);
        stats.put(SystemConstants.Status.READ, readCount != null ? readCount : 0L);
        
        // 统计总消息数量
        Long totalCount = messageMapper.countTotalByUserId(userId);
        stats.put("total", totalCount != null ? totalCount : 0L);
        
        return stats;
    }

    /**
     * 转换为VO对象
     */
    private MessageVO convertToVO(MessageDO message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}