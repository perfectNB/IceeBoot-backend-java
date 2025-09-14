/**
 * 角色菜单关联管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.controller.system;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.system.RoleMenuDTO;
import com.iceeboot.framework.query.system.RoleMenuQuery;
import com.iceeboot.framework.service.system.ISysRoleMenuService;
import com.iceeboot.framework.vo.system.RoleMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色菜单关联管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-13
 */
@Tag(name = "角色菜单管理", description = "角色菜单关联增删改查等接口")
@RestController
@RequestMapping("/v1/role-menus")
@RequiredArgsConstructor
public class SysRoleMenuController {

    private final ISysRoleMenuService roleMenuService;


    @Operation(summary = "分页查询角色菜单关联列表", description = "根据条件分页查询角色菜单关联列表")
    @GetMapping("/page")
    @RequirePermission(dynamic = true)
    public R<PageResult<RoleMenuVO>> getRoleMenuPage(RoleMenuQuery query) {
        PageResult<RoleMenuVO> pageResult = roleMenuService.getRoleMenuPage(query);
        return R.ok(pageResult);
    }


    @Operation(summary = "获取角色菜单关联详情", description = "根据ID获取角色菜单关联详细信息")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<RoleMenuVO> getRoleMenuById(@PathVariable("id") Long id) {
        RoleMenuVO roleMenu = roleMenuService.getRoleMenuById(id);
        return R.ok(roleMenu);
    }


    @Operation(summary = "新增角色菜单关联", description = "创建新的角色菜单关联")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addRoleMenu(@Validated @RequestBody RoleMenuDTO roleMenuDTO) {
        boolean success = roleMenuService.addRoleMenu(roleMenuDTO);
        return success ? R.ok() : R.fail(ResponseMessage.ROLE_MENU_RELATION_CREATE_FAIL);
    }


    @Operation(summary = "更新角色菜单关联", description = "更新角色菜单关联信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateRoleMenu(@Validated @RequestBody RoleMenuDTO roleMenuDTO) {
        boolean success = roleMenuService.updateRoleMenu(roleMenuDTO);
        return success ? R.ok() : R.fail(ResponseMessage.ROLE_MENU_RELATION_UPDATE_FAIL);
    }


    @Operation(summary = "删除角色菜单关联", description = "批量删除角色菜单关联")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteRoleMenus(@RequestBody List<Long> ids) {
        boolean success = roleMenuService.deleteRoleMenus(ids);
        return success ? R.ok() : R.fail(ResponseMessage.ROLE_MENU_RELATION_DELETE_FAIL);
    }


    @Operation(summary = "获取角色的菜单列表", description = "根据角色ID获取该角色的所有菜单ID")
    @GetMapping("/role/{roleId}/menus")
    @RequirePermission(dynamic = true)
    public R<List<Long>> getMenuIdsByRoleId(@PathVariable("roleId") Long roleId) {
        List<Long> menuIds = roleMenuService.getMenuIdsByRoleId(roleId);
        return R.ok(menuIds);
    }

    @Operation(summary = "获取菜单的角色列表", description = "根据菜单ID获取该菜单的所有角色ID")
    @GetMapping("/menu/{menuId}/roles")
    @RequirePermission(dynamic = true)
    public R<List<Long>> getRoleIdsByMenuId(@PathVariable("menuId") Long menuId) {
        List<Long> roleIds = roleMenuService.getRoleIdsByMenuId(menuId);
        return R.ok(roleIds);
    }


    @Operation(summary = "为角色分配菜单", description = "为指定角色分配菜单列表")
    @PostMapping("/role/{roleId}/assign-menus")
    @RequirePermission(dynamic = true)
    public R<Void> assignMenusToRole(@PathVariable("roleId") Long roleId, @RequestBody List<Long> menuIds) {
        boolean success = roleMenuService.assignMenusToRole(roleId, menuIds);
        return success ? R.ok() : R.fail(ResponseMessage.ROLE_MENU_ASSIGN_FAIL);
    }


    @Operation(summary = "移除角色的所有菜单", description = "移除指定角色的所有菜单")
    @DeleteMapping("/role/{roleId}/menus")
    @RequirePermission(dynamic = true)
    public R<Void> removeAllMenusByRoleId(@PathVariable("id") Long roleId) {
        boolean success = roleMenuService.removeAllMenusByRoleId(roleId);
        return success ? R.ok() : R.fail(ResponseMessage.ROLE_MENU_REMOVE_FAIL);
    }
}