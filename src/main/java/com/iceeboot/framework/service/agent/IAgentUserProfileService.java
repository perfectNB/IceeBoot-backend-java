package com.iceeboot.framework.service.agent;

import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.query.agent.MemoryUserQuery;
import com.iceeboot.framework.vo.agent.MemoryView;
import java.util.List;

/** 通用画像的跨用户管理边界，与用户自己的记忆接口分开授权。 */
public interface IAgentUserProfileService {
    PageResult<MemoryView.User> users(long actor, MemoryUserQuery query);
    MemoryView.Profile profile(long actor, long user);
    void edit(long actor, long user, String field, String value, long revision);
    void forget(long actor, long user, String field, long revision);
    List<MemoryView.Candidate> pending(long actor, long user);
    boolean review(long actor, long user, long change, boolean approve);
}
