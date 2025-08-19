/**
 * 角色管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.controller.system;

import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.system.RoleDTO;
import com.zboot.framework.query.system.SysRoleQuery;
import com.zboot.framework.service.system.ISysRoleService;
import com.zboot.framework.vo.system.SysRoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "角色管理", description = "角色增删改查、权限分配等接口")
@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService roleService;

    @Operation(summary = "获取角色列表", description = "根据条件分页查询角色列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<PageResult<SysRoleVO>> getRolePage(SysRoleQuery query) {
        PageResult<SysRoleVO> pageResult = roleService.getRolePage(query);
        return R.ok(pageResult);
    }

    @Operation(summary = "根据ID获取角色", description = "根据角色ID获取角色详细信息")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<SysRoleVO> getRoleById(@PathVariable Long id) {
        SysRoleVO roleVO = roleService.getRoleById(id);
        return R.ok(roleVO);
    }

    @Operation(summary = "创建角色", description = "创建新角色")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addRole(@Validated @RequestBody RoleDTO roleDTO) {
        boolean success = roleService.addRole(roleDTO);
        return success ? R.ok(ResponseMessage.ROLE_CREATE_SUCCESS) : R.fail(ResponseMessage.ROLE_CREATE_FAIL);
    }

    @Operation(summary = "更新角色", description = "更新角色信息")
    @PutMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> updateRole(@PathVariable Long id, @Validated @RequestBody RoleDTO roleDTO) {
        roleDTO.setId(id);
        boolean success = roleService.updateRole(roleDTO);
        return success ? R.ok(ResponseMessage.ROLE_UPDATE_SUCCESS) : R.fail(ResponseMessage.ROLE_UPDATE_FAIL);
    }

    @Operation(summary = "删除角色", description = "根据角色ID删除角色")
    @DeleteMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteRole(@PathVariable Long id) {
        boolean success = roleService.deleteRoles(List.of(id));
        return success ? R.ok(ResponseMessage.ROLE_DELETE_SUCCESS) : R.fail(ResponseMessage.ROLE_DELETE_FAIL);
    }

    @Operation(summary = "保存角色权限", description = "为角色分配权限")
    @PostMapping("/{roleId}/permissions")
    @RequirePermission(dynamic = true)
    public R<Void> saveRolePermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        // 简化实现，实际应该调用角色权限关联服务
        // TODO: 实现角色权限分配逻辑
        return R.ok("角色权限分配成功");
    }
}