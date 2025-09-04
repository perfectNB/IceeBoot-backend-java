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
import com.zboot.framework.service.system.ISysPermissionService;
import com.zboot.framework.vo.system.SysRoleVO;
import com.zboot.framework.vo.system.SysPermissionVO;
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
//    @RequirePermission(dynamic = true)
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

    @Operation(summary = "删除角色", description = "批量删除角色")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteRoles(@RequestBody List<Long> ids) {
        boolean success = roleService.deleteRoles(ids);
        return success ? R.ok(ResponseMessage.ROLE_DELETE_SUCCESS) : R.fail(ResponseMessage.ROLE_DELETE_FAIL);
    }

    @Operation(summary = "修改角色状态", description = "启用或禁用角色")
    @PutMapping("/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        boolean success = roleService.updateStatus(id, status);
        return success ? R.ok(ResponseMessage.STATUS_UPDATE_SUCCESS) : R.fail(ResponseMessage.STATUS_UPDATE_FAIL);
    }



    @Operation(summary = "保存角色权限", description = "为角色分配权限")
    @PostMapping("/{roleId}/permissions")
    @RequirePermission(dynamic = true)
    public R<Void> saveRolePermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        boolean success = roleService.saveRolePermissions(roleId, permissionIds);
        return success ? R.ok("角色权限分配成功") : R.fail("角色权限分配失败");
    }

    @Operation(summary = "保存角色菜单", description = "为角色分配菜单")
    @PostMapping("/{roleId}/menus")
    @RequirePermission(dynamic = true)
    public R<Void> saveRoleMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        boolean success = roleService.saveRoleMenus(roleId, menuIds);
        return success ? R.ok("角色菜单分配成功") : R.fail("角色菜单分配失败");
    }

    @Operation(summary = "获取全部角色", description = "获取全部角色")
    @GetMapping("/getAll")
    @RequirePermission(dynamic = true)
    public R<List<SysRoleVO>> getAllRole() {
        List<SysRoleVO> allRoles = roleService.getAllRoles();
        return R.ok(allRoles);
    }
}