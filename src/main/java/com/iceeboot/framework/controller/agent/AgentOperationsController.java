package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@Tag(name = "智能体运行维护")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/agent-console")
public class AgentOperationsController {
    private final IAgentExecutionService execution;
    private final IAgentQuotaService quota;
    private final IAgentSecretRotationService secrets;
    private final ISysUserService users;

    private String admin() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null || !users.getUserRoles(id).contains("superAdmin"))
            throw new IceeBootException(403, ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
        return id.toString();
    }

    @Operation(summary = "查询当前用户的每日模型调用额度")
    @GetMapping("/quota")
    @RequirePermission(dynamic = true)
    public R<AgentQuotaVO> quota() {
        return R.ok(quota.getQuota(admin()));
    }

    @Operation(summary = "请求取消正在执行的根任务")
    @PostMapping("/runs/{id}/cancel")
    @RequirePermission(dynamic = true)
    public R<Void> cancel(@PathVariable String id) {
        admin();
        execution.cancel(id);
        return R.ok();
    }

    @Operation(summary = "使用部署环境中的当前主密钥重加密存量凭据")
    @PostMapping("/secrets/rotate")
    @RequirePermission(dynamic = true)
    public R<AgentSecretRotationVO> rotate() {
        admin();
        return R.ok(secrets.rotate());
    }
}
