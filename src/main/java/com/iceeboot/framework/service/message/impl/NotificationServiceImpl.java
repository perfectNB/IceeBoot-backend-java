package com.iceeboot.framework.service.message.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iceeboot.common.constant.ApplicationConstants;
import com.iceeboot.config.message.NotificationProperties;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.message.NotificationDTO;
import com.iceeboot.framework.entity.message.NotificationDO;
import com.iceeboot.framework.entity.message.NotificationReadDO;
import com.iceeboot.framework.mapper.message.NotificationMapper;
import com.iceeboot.framework.mapper.message.NotificationReadMapper;
import com.iceeboot.framework.query.message.NotificationQuery;
import com.iceeboot.framework.service.message.INotificationService;
import com.iceeboot.framework.vo.message.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private final NotificationProperties notificationProperties;
    private final NotificationMapper notificationMapper;
    private final NotificationReadMapper notificationReadMapper;

    @Override
    public PageResult<NotificationVO> getAdminPage(NotificationQuery query) {
        LambdaQueryWrapper<NotificationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                .eq(StringUtils.hasText(query.getApplicationCode()), NotificationDO::getApplicationCode,
                        normalizeApplicationCode(query.getApplicationCode()))
                .eq(StringUtils.hasText(query.getType()), NotificationDO::getType, query.getType())
                .eq(StringUtils.hasText(query.getStatus()), NotificationDO::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), nested -> nested
                        .like(NotificationDO::getTitle, query.getKeyword())
                        .or().like(NotificationDO::getContent, query.getKeyword()))
                .orderByDesc(NotificationDO::getCreateTime);
        IPage<NotificationDO> page = notificationMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page, Set.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(NotificationDTO dto) {
        String applicationCode = normalizeApplicationCode(dto.getApplicationCode());
        validateApplicationCode(applicationCode);
        NotificationDO notification = dto.getId() == null
                ? new NotificationDO()
                : notificationMapper.selectById(dto.getId());
        if (notification == null) {
            throw new IceeBootException("通知不存在");
        }
        if (STATUS_PUBLISHED.equals(notification.getStatus())) {
            throw new IceeBootException("已发布通知请先撤回再修改");
        }
        BeanUtils.copyProperties(dto, notification, "id");
        notification.setApplicationCode(applicationCode);
        notification.setStatus(STATUS_DRAFT);
        notification.setPublishTime(null);
        notification.setDelFlag(SystemConstants.DeleteFlag.NOT_DELETED);
        if (notification.getId() == null) {
            notificationMapper.insert(notification);
        } else {
            notificationMapper.updateById(notification);
        }
        return notification.getId();
    }

    @Override
    public boolean publish(Long id) {
        NotificationDO notification = requireNotification(id);
        notification.setStatus(STATUS_PUBLISHED);
        notification.setPublishTime(LocalDateTime.now());
        return notificationMapper.updateById(notification) == 1;
    }

    @Override
    public boolean withdraw(Long id) {
        NotificationDO notification = requireNotification(id);
        notification.setStatus(STATUS_WITHDRAWN);
        return notificationMapper.updateById(notification) == 1;
    }

    @Override
    public boolean delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return true;
        return notificationMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public PageResult<NotificationVO> getUserPage(String applicationCode, NotificationQuery query) {
        Long userId = requireCurrentUserId();
        String normalizedCode = normalizeApplicationCode(applicationCode);
        validateUserApplicationCode(normalizedCode);
        IPage<NotificationDO> page = notificationMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                buildVisibleWrapper(normalizedCode, userId, query.getType()));
        Set<Long> readIds = getReadIds(userId, page.getRecords().stream().map(NotificationDO::getId).toList());
        return toPageResult(page, readIds);
    }

    @Override
    public long getUnreadCount(String applicationCode) {
        Long userId = requireCurrentUserId();
        String normalizedCode = normalizeApplicationCode(applicationCode);
        validateUserApplicationCode(normalizedCode);
        List<Long> visibleIds = notificationMapper.selectList(buildVisibleWrapper(normalizedCode, userId, null))
                .stream().map(NotificationDO::getId).toList();
        return visibleIds.size() - getReadIds(userId, visibleIds).size();
    }

    @Override
    public NotificationVO getLatestUnreadImportant(String applicationCode) {
        Long userId = requireCurrentUserId();
        String normalizedCode = normalizeApplicationCode(applicationCode);
        validateUserApplicationCode(normalizedCode);
        List<NotificationDO> notices = notificationMapper.selectList(
                buildVisibleWrapper(normalizedCode, userId, null)).stream()
                .filter(item -> "system".equalsIgnoreCase(item.getType())
                        || "warning".equalsIgnoreCase(item.getType()))
                .toList();
        if (notices.isEmpty()) return null;
        Set<Long> readIds = getReadIds(userId, notices.stream().map(NotificationDO::getId).toList());
        return notices.stream().filter(item -> !readIds.contains(item.getId())).findFirst()
                .map(item -> {
                    NotificationVO vo = new NotificationVO();
                    BeanUtils.copyProperties(item, vo);
                    vo.setRead(false);
                    return vo;
                }).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRead(Long id, String applicationCode) {
        Long userId = requireCurrentUserId();
        validateUserApplicationCode(normalizeApplicationCode(applicationCode));
        if (notificationMapper.selectCount(buildVisibleWrapper(
                normalizeApplicationCode(applicationCode), userId, null).eq(NotificationDO::getId, id)) == 0) {
            throw new IceeBootException("通知不存在或无权访问");
        }
        return insertReadIfAbsent(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllRead(String applicationCode) {
        Long userId = requireCurrentUserId();
        validateUserApplicationCode(normalizeApplicationCode(applicationCode));
        List<Long> ids = notificationMapper.selectList(buildVisibleWrapper(
                        normalizeApplicationCode(applicationCode), userId, null))
                .stream().map(NotificationDO::getId).toList();
        Set<Long> readIds = getReadIds(userId, ids);
        ids.stream().filter(id -> !readIds.contains(id)).sorted().forEach(id -> insertReadIfAbsent(id, userId));
        return true;
    }

    private LambdaQueryWrapper<NotificationDO> buildVisibleWrapper(String applicationCode,
                                                                    Long userId,
                                                                    String type) {
        LocalDateTime now = LocalDateTime.now();
        return new LambdaQueryWrapper<NotificationDO>()
                .eq(NotificationDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                .eq(NotificationDO::getStatus, STATUS_PUBLISHED)
                .le(NotificationDO::getPublishTime, now)
                .and(nested -> nested.isNull(NotificationDO::getExpireTime)
                        .or().gt(NotificationDO::getExpireTime, now))
                .and(nested -> nested.eq(NotificationDO::getApplicationCode, ApplicationConstants.ALL)
                        .or().eq(NotificationDO::getApplicationCode, applicationCode))
                .and(nested -> nested.isNull(NotificationDO::getTargetUserId)
                        .or().eq(NotificationDO::getTargetUserId, userId))
                .eq(StringUtils.hasText(type), NotificationDO::getType, type)
                .orderByDesc(NotificationDO::getPublishTime);
    }

    private Set<Long> getReadIds(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return Set.of();
        List<NotificationReadDO> reads = notificationReadMapper.selectList(
                new LambdaQueryWrapper<NotificationReadDO>()
                        .eq(NotificationReadDO::getUserId, userId)
                        .in(NotificationReadDO::getNotificationId, notificationIds)
                        .eq(NotificationReadDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
        Set<Long> ids = new HashSet<>();
        reads.forEach(read -> ids.add(read.getNotificationId()));
        return ids;
    }

    private boolean insertReadIfAbsent(Long notificationId, Long userId) {
        if (notificationMapper.selectByIdForUpdate(notificationId) == null) {
            throw new IceeBootException("通知不存在");
        }
        Long count = notificationReadMapper.selectCount(new LambdaQueryWrapper<NotificationReadDO>()
                .eq(NotificationReadDO::getNotificationId, notificationId)
                .eq(NotificationReadDO::getUserId, userId)
                .eq(NotificationReadDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
        if (count != null && count > 0) return true;
        NotificationReadDO read = new NotificationReadDO();
        read.setNotificationId(notificationId);
        read.setUserId(userId);
        read.setReadTime(LocalDateTime.now());
        read.setDelFlag(SystemConstants.DeleteFlag.NOT_DELETED);
        try {
            return notificationReadMapper.insert(read) == 1;
        } catch (DuplicateKeyException ignored) {
            return true;
        }
    }

    private PageResult<NotificationVO> toPageResult(IPage<NotificationDO> page, Set<Long> readIds) {
        List<NotificationVO> records = page.getRecords().stream().map(item -> {
            NotificationVO vo = new NotificationVO();
            BeanUtils.copyProperties(item, vo);
            vo.setRead(readIds.contains(item.getId()));
            return vo;
        }).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private NotificationDO requireNotification(Long id) {
        NotificationDO notification = id == null ? null : notificationMapper.selectById(id);
        if (notification == null) throw new IceeBootException("通知不存在");
        return notification;
    }

    private Long requireCurrentUserId() {
        Long userId = TokenUtils.getCurrentUserId();
        if (userId == null) throw new IceeBootException("请先登录");
        return userId;
    }

    private String normalizeApplicationCode(String code) {
        return StringUtils.hasText(code) ? code.trim().toUpperCase(java.util.Locale.ROOT) : ApplicationConstants.PLATFORM;
    }

    private void validateApplicationCode(String code) {
        if (!ApplicationConstants.ALL.equals(code) && !notificationProperties.getApplicationCodes().contains(code)) throw new IceeBootException("不支持的项目编码");
    }

    private void validateUserApplicationCode(String code) {
        if (ApplicationConstants.ALL.equals(code) || !notificationProperties.getApplicationCodes().contains(code)) {
            throw new IceeBootException("无效的项目编码");
        }
    }
}
