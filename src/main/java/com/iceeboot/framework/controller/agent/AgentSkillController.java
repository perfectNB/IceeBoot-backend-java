package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.agent.*;
import com.iceeboot.framework.vo.agent.AgentSkillImportPreviewVO;
import com.iceeboot.framework.service.agent.IAgentSkillImportService;
import com.iceeboot.framework.dto.update.agent.AgentSkillUpdateDTO;
import com.iceeboot.framework.service.agent.IAgentSkillService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.AgentSkillVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 智能体技能管理控制器。 */
@Tag(name = "智能体技能管理")
@RestController
@RequestMapping("/v1/agent-console/skills")
@RequiredArgsConstructor
public class AgentSkillController {
    private final IAgentSkillService service;
    private final ISysUserService users;
    private final IAgentSkillImportService imports;

    private void admin() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null || !users.getUserRoles(id).contains("superAdmin"))
            throw new IceeBootException(
                    ResultCodeEnum.FORBIDDEN.getCode(), ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
    }

    @Operation(summary = "查询技能列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<List<AgentSkillVO>> getResourceList() {
        admin();
        return R.ok(service.getResourceList());
    }

    @Operation(summary = "查看技能详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<AgentSkillVO> getResourceById(@PathVariable long id) {
        admin();
        return R.ok(service.getResourceById(id));
    }

    @Operation(summary = "新增技能")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<AgentSkillVO> create(@Valid @RequestBody AgentSkillDTO request) {
        admin();
        return R.ok(service.addResource(request));
    }

    @Operation(summary = "更新技能")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<AgentSkillVO> update(@Valid @RequestBody AgentSkillUpdateDTO request) {
        admin();
        return R.ok(service.updateResource(request));
    }

    @Operation(summary = "删除技能")
    @DeleteMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> delete(@PathVariable long id) {
        admin();
        service.deleteResource(id);
        return R.ok();
    }

    @Operation(summary = "预览技能压缩包")
    @PostMapping(value = "/import/archive", consumes = "multipart/form-data")
    @RequirePermission(dynamic = true)
    public R<AgentSkillImportPreviewVO> previewArchive(@Valid @ModelAttribute AgentSkillArchiveDTO request) {
        admin();
        return R.ok(imports.previewArchive(request));
    }

    @Operation(summary = "预览GitHub技能")
    @PostMapping("/import/github")
    @RequirePermission(dynamic = true)
    public R<AgentSkillImportPreviewVO> previewGithub(@Valid @RequestBody AgentSkillGithubDTO request) {
        admin();
        return R.ok(imports.previewGithub(request));
    }

    @Operation(summary = "确认导入技能")
    @PostMapping("/import")
    @RequirePermission(dynamic = true)
    public R<List<AgentSkillVO>> importSkills(@Valid @RequestBody AgentSkillImportDTO request) {
        admin();
        return R.ok(imports.commit(request));
    }
}
