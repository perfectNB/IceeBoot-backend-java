package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.agent.AgentToolDTO;
import com.iceeboot.framework.dto.update.agent.AgentToolUpdateDTO;
import com.iceeboot.framework.service.agent.IAgentToolService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.AgentToolVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 智能体工具管理控制器。 */
@Tag(name = "智能体工具管理")
@RestController
@RequestMapping("/v1/agent-console/tools")
@RequiredArgsConstructor
public class AgentToolController {
    private final IAgentToolService service;
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
    public R<List<AgentToolVO>> getResourceList() {
        admin();
        return R.ok(service.getResourceList());
    }

    @Operation(summary = "查询资源详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<AgentToolVO> getResourceById(@PathVariable long id) {
        admin();
        return R.ok(service.getResourceById(id));
    }

    @Operation(summary = "新增资源")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<AgentToolVO> create(@Valid @RequestBody AgentToolDTO request) {
        admin();
        return R.ok(service.addResource(request));
    }

    @Operation(summary = "更新资源")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<AgentToolVO> update(@Valid @RequestBody AgentToolUpdateDTO request) {
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
