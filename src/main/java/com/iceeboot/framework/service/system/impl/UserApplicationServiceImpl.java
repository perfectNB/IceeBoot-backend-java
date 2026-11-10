package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.system.UserApplicationDO;
import com.iceeboot.framework.mapper.system.SysUserMapper;
import com.iceeboot.framework.mapper.system.UserApplicationMapper;
import com.iceeboot.framework.service.system.IUserApplicationService;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationServiceImpl implements IUserApplicationService {

    private final UserApplicationMapper userApplicationMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enterApplication(Long userId, String applicationCode) {
        if (userId == null || !StringUtils.hasText(applicationCode)) {
            throw new IceeBootException("用户和应用编码不能为空");
        }
        applicationCode = applicationCode.trim().toUpperCase(Locale.ROOT);
        if (userMapper.selectByIdForUpdate(userId) == null) {
            throw new IceeBootException("用户不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        UserApplicationDO existing = userApplicationMapper.selectOne(
                new LambdaQueryWrapper<UserApplicationDO>()
                        .eq(UserApplicationDO::getUserId, userId)
                        .eq(UserApplicationDO::getApplicationCode, applicationCode)
                        .eq(UserApplicationDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
        if (existing != null) {
            existing.setLastLoginTime(now);
            userApplicationMapper.updateById(existing);
            return false;
        }

        UserApplicationDO relation = new UserApplicationDO()
                .setUserId(userId)
                .setApplicationCode(applicationCode)
                .setStatus(SystemConstants.Status.ACTIVE)
                .setFirstLoginTime(now)
                .setLastLoginTime(now);
        relation.setDelFlag(SystemConstants.DeleteFlag.NOT_DELETED);
        try {
            userApplicationMapper.insert(relation);
        } catch (DuplicateKeyException ignored) {
            return false;
        }

        return true;
    }
}
