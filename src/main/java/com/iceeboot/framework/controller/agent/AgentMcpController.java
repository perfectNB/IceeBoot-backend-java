package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.agent.AgentMcpDTO;
import com.iceeboot.framework.dto.update.agent.AgentMcpUpdateDTO;
import com.iceeboot.framework.service.agent.IAgentMcpService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.AgentMcpVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 智能体MCP 服务管理控制器。 */
@Tag(name = "智能体MCP 服务管理")
@RestController
@RequestMapping("/v1/agent-console/mcps")
@RequiredArgsConstructor
public class AgentMcpController {
    private final IAgentMcpService service;
    private final ISysUserService users;

    private void admin() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null || !users.getUserRoles(id).contains("superAdmin"))
            throw new IceeBootException(
                    ResultCodeEnum.FORBIDDEN.getCode(), ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
    }

    @Operation(summary = "查询资源列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<List<AgentMcpVO>> getResourceList() {
        admin();
        return R.ok(service.getResourceList());
    }

    @Operation(summary = "查询资源详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<AgentMcpVO> getResourceById(@PathVariable long id) {
        admin();
        return R.ok(service.getResourceById(id));
    }

    @Operation(summary = "新增资源")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<AgentMcpVO> create(@Valid @RequestBody AgentMcpDTO request) {
        admin();
        return R.ok(service.addResource(request));
    }

    @Operation(summary = "更新资源")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<AgentMcpVO> update(@Valid @RequestBody AgentMcpUpdateDTO request) {
        admin();
        return R.ok(service.updateResource(request));
    }

    @Operation(summary = "删除资源")
    @DeleteMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> delete(@PathVariable long id) {
        admin();
        service.deleteResource(id);
        return R.ok();
    }
}
