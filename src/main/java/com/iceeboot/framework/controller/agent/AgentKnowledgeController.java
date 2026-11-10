package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.agent.AgentKnowledgeDTO;
import com.iceeboot.framework.dto.update.agent.AgentKnowledgeUpdateDTO;
import com.iceeboot.framework.service.agent.IAgentKnowledgeService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.AgentKnowledgeVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 智能体知识库连接管理控制器。 */
@Tag(name = "智能体知识库连接管理")
@RestController
@RequestMapping("/v1/agent-console/knowledge")
@RequiredArgsConstructor
public class AgentKnowledgeController {
    private final IAgentKnowledgeService service;
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
    public R<List<AgentKnowledgeVO>> getResourceList() {
        admin();
        return R.ok(service.getResourceList());
    }

    @Operation(summary = "查询资源详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<AgentKnowledgeVO> getResourceById(@PathVariable long id) {
        admin();
        return R.ok(service.getResourceById(id));
    }

    @Operation(summary = "新增资源")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<AgentKnowledgeVO> create(@Valid @RequestBody AgentKnowledgeDTO request) {
        admin();
        return R.ok(service.addResource(request));
    }

    @Operation(summary = "更新资源")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<AgentKnowledgeVO> update(@Valid @RequestBody AgentKnowledgeUpdateDTO request) {
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
