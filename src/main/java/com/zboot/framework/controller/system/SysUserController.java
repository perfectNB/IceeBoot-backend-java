/**
 * 用户管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.controller.system;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.system.SysUserDTO;
import com.zboot.framework.query.system.SysUserQuery;
import com.zboot.framework.service.system.ISysUserService;
import com.zboot.framework.vo.system.SysUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "用户管理", description = "用户增删改查、状态管理等接口")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    @Autowired
    private ISysUserService userService;

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户列表", description = "根据条件分页查询用户列表")
    @GetMapping("/page")
    public R<PageResult<SysUserVO>> getUserPage(SysUserQuery query) {
        PageResult<SysUserVO> pageResult = userService.getUserPage(query);
        return R.ok(pageResult);
    }

    /**
     * 根据ID获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{id}")
    public R<SysUserVO> getUserById(@PathVariable Long id) {
        SysUserVO userVO = userService.getUserById(id);
        return R.ok(userVO);
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户", description = "创建新用户")
    @PostMapping
    public R<Void> addUser(@Validated @RequestBody SysUserDTO userDTO) {
        boolean success = userService.addUser(userDTO);
        return success ? R.ok("用户创建成功") : R.fail("用户创建失败");
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping
    public R<Void> updateUser(@Validated @RequestBody SysUserDTO userDTO) {
        boolean success = userService.updateUser(userDTO);
        return success ? R.ok("用户更新成功") : R.fail("用户更新失败");
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "批量删除用户")
    @DeleteMapping
    public R<Void> deleteUsers(@RequestBody List<Long> ids) {
        boolean success = userService.deleteUsers(ids);
        return success ? R.ok("用户删除成功") : R.fail("用户删除失败");
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "重置用户密码")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        boolean success = userService.resetPassword(id, newPassword);
        return success ? R.ok("密码重置成功") : R.fail("密码重置失败");
    }

    /**
     * 修改用户状态
     */
    @Operation(summary = "修改用户状态", description = "启用或禁用用户")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        boolean success = userService.updateStatus(id, status);
        return success ? R.ok("状态修改成功") : R.fail("状态修改失败");
    }

    /**
     * 检查用户名是否存在
     */
    @Operation(summary = "检查用户名", description = "检查用户名是否已存在")
    @GetMapping("/check/username")
    public R<Boolean> checkUsername(@RequestParam String username, @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsUsername(username, excludeId);
        return R.ok(exists);
    }

    /**
     * 检查手机号是否存在
     */
    @Operation(summary = "检查手机号", description = "检查手机号是否已存在")
    @GetMapping("/check/phone")
    public R<Boolean> checkPhone(@RequestParam String phone, @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsPhone(phone, excludeId);
        return R.ok(exists);
    }

    /**
     * 检查邮箱是否存在
     */
    @Operation(summary = "检查邮箱", description = "检查邮箱是否已存在")
    @GetMapping("/check/email")
    public R<Boolean> checkEmail(@RequestParam String email, @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsEmail(email, excludeId);
        return R.ok(exists);
    }
}