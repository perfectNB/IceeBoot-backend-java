package com.iceeboot.framework.service.agent.schedule;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.model.agent.PublishedAgent;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.system.IAuthCacheService;
import com.iceeboot.framework.service.system.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** 请求和后台触发共用实时授权，后台身份不能携带用户登录令牌。 */
@Component
@RequiredArgsConstructor
public class AgentScheduleAccess {
    private final IAuthCacheService auth;
    private final ISysUserService users;
    private final IAgentPersistenceService store;

    public void requireAdmin(Long owner) {
        if (owner == null || !auth.hasUserPermissions(owner, List.of("ai:schedule:read:api"), true)
                || !users.getUserRoles(owner).contains("superAdmin"))
            throw new IceeBootException(403, ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
    }

    public void requireExecution(long owner) {
        requireAdmin(owner);
        List<String> invoke = auth.getUrlPermissions("/v1/agent-console/agents/by-code/scheduled/invoke", "POST");
        var codes = new ArrayList<>(invoke);
        codes.add("ai:schedule:execute");
        if (invoke.isEmpty() || !auth.hasUserPermissions(owner, codes, true))
            throw new IceeBootException(403, ResponseMessage.Agent.SCHEDULE_PERMISSION_DENIED);
    }

    public void requireAgent(String code) {
        var view = store.agentByCode(code);
        PublishedAgent release = store.release(view.activeReleaseId());
        if (!release.agent().enabled())
            throw new IceeBootException(ResponseMessage.Agent.AGENT_RELEASE_DISABLED);
        checkBackgroundTools(release);
    }

    private void checkBackgroundTools(PublishedAgent release) {
        if (release.capabilities().stream().anyMatch(capability ->
                capability.tool() != null && capability.tool().projectApi()))
            throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_PROJECT_TOOL_UNSUPPORTED);
        for (PublishedAgent child : release.children()) checkBackgroundTools(child);
    }
}
