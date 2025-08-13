/**
 * 用户角色关联管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.controller.system;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.system.UserRoleDTO;
import com.zboot.framework.query.system.UserRoleQry;
import com.zboot.framework.service.system.ISysUserRoleService;
import com.zboot.framework.vo.system.UserRoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户角色关联管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Tag(name = "用户角色管理", description = "用户角色关联增删改查等接口")
@RestController
@RequestMapping("/v1/user-roles")
@RequiredArgsConstructor
public class SysUserRoleController {

    private final ISysUserRoleService userRoleService;

    /**
     * 分页查询用户角色关联列表
     */
    @Operation(summary = "分页查询用户角色关联列表", description = "根据条件分页查询用户角色关联列表")
    @GetMapping("/page")
    public R<PageResult<UserRoleVO>> getUserRolePage(UserRoleQry query) {
        PageResult<UserRoleVO> pageResult = userRoleService.getUserRolePage(query);
        return R.ok(pageResult);
    }

    /**
     * 获取用户角色关联详情
     */
    @Operation(summary = "获取用户角色关联详情", description = "根据ID获取用户角色关联详细信息")
    @GetMapping("/{id}")
    public R<UserRoleVO> getUserRoleById(@PathVariable Long id) {
        UserRoleVO userRole = userRoleService.getUserRoleById(id);
        return R.ok(userRole);
    }

    /**
     * 新增用户角色关联
     */
    @Operation(summary = "新增用户角色关联", description = "创建新的用户角色关联")
    @PostMapping
    public R<Void> addUserRole(@Validated @RequestBody UserRoleDTO userRoleDTO) {
        boolean success = userRoleService.addUserRole(userRoleDTO);
        return success ? R.ok() : R.fail("新增用户角色关联失败");
    }

    /**
     * 更新用户角色关联
     */
    @Operation(summary = "更新用户角色关联", description = "更新用户角色关联信息")
    @PutMapping
    public R<Void> updateUserRole(@Validated @RequestBody UserRoleDTO userRoleDTO) {
        boolean success = userRoleService.updateUserRole(userRoleDTO);
        return success ? R.ok() : R.fail("更新用户角色关联失败");
    }

    /**
     * 删除用户角色关联
     */
    @Operation(summary = "删除用户角色关联", description = "批量删除用户角色关联")
    @DeleteMapping
    public R<Void> deleteUserRoles(@RequestBody List<Long> ids) {
        boolean success = userRoleService.deleteUserRoles(ids);
        return success ? R.ok() : R.fail("删除用户角色关联失败");
    }

    /**
     * 根据用户ID获取角色ID列表
     */
    @Operation(summary = "获取用户的角色列表", description = "根据用户ID获取该用户的所有角色ID")
    @GetMapping("/user/{userId}/roles")
    public R<List<Long>> getRoleIdsByUserId(@PathVariable Long userId) {
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(userId);
        return R.ok(roleIds);
    }

    /**
     * 根据角色ID获取用户ID列表
     */
    @Operation(summary = "获取角色的用户列表", description = "根据角色ID获取该角色的所有用户ID")
    @GetMapping("/role/{roleId}/users")
    public R<List<Long>> getUserIdsByRoleId(@PathVariable Long roleId) {
        List<Long> userIds = userRoleService.getUserIdsByRoleId(roleId);
        return R.ok(userIds);
    }

    /**
     * 为用户分配角色
     */
    @Operation(summary = "为用户分配角色", description = "为指定用户分配角色列表")
    @PostMapping("/user/{userId}/assign-roles")
    public R<Void> assignRolesToUser(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        boolean success = userRoleService.assignRolesToUser(userId, roleIds);
        return success ? R.ok() : R.fail("分配角色失败");
    }

    /**
     * 移除用户的所有角色
     */
    @Operation(summary = "移除用户的所有角色", description = "移除指定用户的所有角色")
    @DeleteMapping("/user/{userId}/roles")
    public R<Void> removeAllRolesByUserId(@PathVariable Long userId) {
        boolean success = userRoleService.removeAllRolesByUserId(userId);
        return success ? R.ok() : R.fail("移除用户角色失败");
    }
}