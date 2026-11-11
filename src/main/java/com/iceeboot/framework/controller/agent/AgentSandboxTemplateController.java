package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.agent.AgentSandboxTemplateDTO;
import com.iceeboot.framework.dto.update.agent.AgentSandboxTemplateUpdateDTO;
import com.iceeboot.framework.service.agent.IAgentSandboxTemplateService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.AgentSandboxTemplateVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 智能体沙箱模板管理控制器。 */
@Tag(name = "智能体沙箱模板管理")
@RestController
@RequestMapping("/v1/agent-console/sandbox-templates")
@RequiredArgsConstructor
public class AgentSandboxTemplateController {
    private final IAgentSandboxTemplateService service;
    private final ISysUserService users;

    private void admin() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null || !users.getUserRoles(id).contains("superAdmin"))
            throw new IceeBootException(
                    ResultCodeEnum.FORBIDDEN.getCode(), ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
    }

    @Operation(summary = "查询沙箱模板列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<List<AgentSandboxTemplateVO>> getResourceList() {
        admin();
        return R.ok(service.getResourceList());
    }

    @Operation(summary = "查看沙箱模板详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<AgentSandboxTemplateVO> getResourceById(@PathVariable long id) {
        admin();
        return R.ok(service.getResourceById(id));
    }

    @Operation(summary = "新增沙箱模板")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<AgentSandboxTemplateVO> create(@Valid @RequestBody AgentSandboxTemplateDTO request) {
        admin();
        return R.ok(service.addResource(request));
    }

    @Operation(summary = "更新沙箱模板")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<AgentSandboxTemplateVO> update(
            @Valid @RequestBody AgentSandboxTemplateUpdateDTO request) {
        admin();
        return R.ok(service.updateResource(request));
    }

    @Operation(summary = "删除沙箱模板")
    @DeleteMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> delete(@PathVariable long id) {
        admin();
        service.deleteResource(id);
        return R.ok();
    }
}
