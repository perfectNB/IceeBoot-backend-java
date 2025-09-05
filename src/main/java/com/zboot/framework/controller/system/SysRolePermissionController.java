/**
 * 角色权限关联管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.controller.system;

import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.create.system.RolePermissionDTO;
import com.zboot.framework.query.system.RolePermissionQuery;
import com.zboot.framework.service.system.ISysRolePermissionService;
import com.zboot.framework.vo.system.RolePermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色权限关联管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-13
 */
@Tag(name = "角色权限管理", description = "角色权限关联增删改查等接口")
@RestController
@RequestMapping("/v1/role-permissions")
@RequiredArgsConstructor
public class SysRolePermissionController {

    private final ISysRolePermissionService rolePermissionService;


    @Operation(summary = "分页查询角色权限关联列表", description = "根据条件分页查询角色权限关联列表")
    @GetMapping("/page")
    @RequirePermission(dynamic = true)
    public R<PageResult<RolePermissionVO>> getRolePermissionPage(RolePermissionQuery query) {
        PageResult<RolePermissionVO> pageResult = rolePermissionService.getRolePermissionPage(query);
        return R.ok(pageResult);
    }


    @Operation(summary = "获取角色权限关联详情", description = "根据ID获取角色权限关联详细信息")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<RolePermissionVO> getRolePermissionById(@PathVariable Long id) {
        RolePermissionVO rolePermission = rolePermissionService.getRolePermissionById(id);
        return R.ok(rolePermission);
    }


    @Operation(summary = "新增角色权限关联", description = "创建新的角色权限关联")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addRolePermission(@Validated @RequestBody RolePermissionDTO rolePermissionDTO) {
        boolean success = rolePermissionService.addRolePermission(rolePermissionDTO);
        return success ? R.ok() : R.fail("新增角色权限关联失败");
    }


    @Operation(summary = "更新角色权限关联", description = "更新角色权限关联信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateRolePermission(@Validated @RequestBody RolePermissionDTO rolePermissionDTO) {
        boolean success = rolePermissionService.updateRolePermission(rolePermissionDTO);
        return success ? R.ok() : R.fail("更新角色权限关联失败");
    }


    @Operation(summary = "删除角色权限关联", description = "批量删除角色权限关联")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteRolePermissions(@RequestBody List<Long> ids) {
        boolean success = rolePermissionService.deleteRolePermissions(ids);
        return success ? R.ok() : R.fail("删除角色权限关联失败");
    }


    @Operation(summary = "获取角色的权限列表", description = "根据角色ID获取该角色的所有权限ID")
    @GetMapping("/role/{roleId}/permissions")
    @RequirePermission(dynamic = true)
    public R<List<Long>> getPermissionIdsByRoleId(@PathVariable Long roleId) {
        List<Long> permissionIds = rolePermissionService.getPermissionIdsByRoleId(roleId);
        return R.ok(permissionIds);
    }


    @Operation(summary = "获取权限的角色列表", description = "根据权限ID获取该权限的所有角色ID")
    @GetMapping("/permission/{permissionId}/roles")
    @RequirePermission(dynamic = true)
    public R<List<Long>> getRoleIdsByPermissionId(@PathVariable Long permissionId) {
        List<Long> roleIds = rolePermissionService.getRoleIdsByPermissionId(permissionId);
        return R.ok(roleIds);
    }


    @Operation(summary = "为角色分配权限", description = "为指定角色分配权限列表")
    @PostMapping("/role/{roleId}/assign-permissions")
    @RequirePermission(dynamic = true)
    public R<Void> assignPermissionsToRole(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        boolean success = rolePermissionService.assignPermissionsToRole(roleId, permissionIds);
        return success ? R.ok() : R.fail("分配权限失败");
    }


    @Operation(summary = "移除角色的所有权限", description = "移除指定角色的所有权限")
    @DeleteMapping("/role/{roleId}/permissions")
    @RequirePermission(dynamic = true)
    public R<Void> removeAllPermissionsByRoleId(@PathVariable Long roleId) {
        boolean success = rolePermissionService.removeAllPermissionsByRoleId(roleId);
        return success ? R.ok() : R.fail("移除角色权限失败");
    }
}