package com.iceeboot.framework.service.agent.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.enums.agent.MemoryScope;
import com.iceeboot.framework.mapper.system.SysUserMapper;
import com.iceeboot.framework.query.agent.MemoryUserQuery;
import com.iceeboot.framework.service.agent.IAgentUserProfileService;
import com.iceeboot.framework.service.system.IAuthCacheService;
import com.iceeboot.framework.vo.agent.MemoryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoryUserProfileService implements IAgentUserProfileService {
    private final SysUserMapper users;
    private final IAuthCacheService auth;
    private final MemoryProfileService profiles;
    private final MemoryChangeService changes;

    private void require(long actor, String action) {
        if (actor <= 0 || !auth.hasUserPermissions(actor, List.of("ai:memory:user-" + action + ":api"), true))
            throw new IceeBootException(403, ResponseMessage.Agent.PERMISSION_DENIED);
    }

    private String target(long user) {
        if (user <= 0 || users.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getId, user).eq(UserDO::getDelFlag, 0)) == 0)
            throw new IceeBootException(404, ResponseMessage.Agent.MEMORY_USER_NOT_FOUND);
        return Long.toString(user);
    }

    @Override
    public PageResult<MemoryView.User> users(long actor, MemoryUserQuery query) {
        require(actor, "list");
        if (query == null || query.getPageNum() == null || query.getPageSize() == null
                || query.getPageNum() < 1 || query.getPageNum() > 100000 || query.getPageSize() < 1 || query.getPageSize() > 100
                || (query.getKeyword() != null && query.getKeyword().length() > 100))
            throw new IceeBootException(ResponseMessage.Agent.REQUEST_INVALID);
        var criteria = new LambdaQueryWrapper<UserDO>().select(UserDO::getId, UserDO::getUsername,
                UserDO::getNickname, UserDO::getStatus).eq(UserDO::getDelFlag, 0).orderByAsc(UserDO::getId);
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        if (!keyword.isBlank()) criteria.and(group -> group.like(UserDO::getUsername, keyword).or().like(UserDO::getNickname, keyword));
        var page = users.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), criteria);
        return PageResult.of(page.getRecords().stream().map(row -> new MemoryView.User(row.getId().toString(),
                row.getUsername(), row.getNickname(), row.getStatus())).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public MemoryView.Profile profile(long actor, long user) {
        require(actor, "read");
        return profiles.view(MemoryScope.USER, 0, target(user));
    }

    @Override
    public void edit(long actor, long user, String field, String value, long revision) {
        require(actor, "edit");
        profiles.editUser(target(user), Long.toString(actor), field, value, revision);
    }

    @Override
    public void forget(long actor, long user, String field, long revision) {
        require(actor, "forget");
        profiles.editUser(target(user), Long.toString(actor), field, null, revision);
    }

    @Override
    public List<MemoryView.Candidate> pending(long actor, long user) {
        require(actor, "review-read");
        return changes.pending(MemoryScope.USER, 0, target(user));
    }

    @Override
    public boolean review(long actor, long user, long change, boolean approve) {
        require(actor, "review");
        return changes.reviewUser(target(user), Long.toString(actor), change, approve);
    }
}
