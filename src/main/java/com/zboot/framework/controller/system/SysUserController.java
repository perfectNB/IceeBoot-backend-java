/**
 * 用户管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.controller.system;

import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.create.system.UserDTO;
import com.zboot.framework.dto.update.system.UserUpdateDTO;
import com.zboot.framework.query.system.SysUserQuery;
import com.zboot.framework.service.system.ISysUserService;
import com.zboot.framework.vo.system.SysUserVO;
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
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class SysUserController {


    private final ISysUserService userService;


    @Operation(summary = "分页查询用户列表", description = "根据条件分页查询用户列表")
    @GetMapping()
//    @RequirePermission(dynamic = true)
    public R<PageResult<SysUserVO>> getUserPage(SysUserQuery query) {
        PageResult<SysUserVO> pageResult = userService.getUserPage(query);
        return R.ok(pageResult);
    }


    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{id}")
//    @RequirePermission(dynamic = true)
    public R<SysUserVO> getUserById(@PathVariable Long id) {
        SysUserVO userVO = userService.getUserById(id);
        return R.ok(userVO);
    }


    @Operation(summary = "新增用户", description = "创建新用户")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addUser(@Validated @RequestBody UserDTO userDTO) {
        boolean success = userService.addUser(userDTO);
        return success ? R.ok(ResponseMessage.USER_CREATE_SUCCESS) : R.fail(ResponseMessage.USER_CREATE_FAIL);
    }


    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping
//    @RequirePermission(dynamic = true)
    public R<Void> updateUser(@Validated @RequestBody UserUpdateDTO dto) {
        boolean success = userService.updateUser(dto);
        return success ? R.ok(ResponseMessage.USER_UPDATE_SUCCESS) : R.fail(ResponseMessage.USER_UPDATE_FAIL);
    }

    @Operation(summary = "删除用户", description = "批量删除用户")
    @DeleteMapping
//    @RequirePermission(dynamic = true)
    public R<Void> deleteUsers(@RequestBody List<Long> ids) {
        boolean success = userService.deleteUsers(ids);
        return success ? R.ok(ResponseMessage.USER_DELETE_SUCCESS) : R.fail(ResponseMessage.USER_DELETE_FAIL);
    }


    @Operation(summary = "重置密码", description = "重置用户密码")
    @PutMapping("/{id}/password")
//    @RequirePermission(dynamic = true)
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        boolean success = userService.resetPassword(id, newPassword);
        return success ? R.ok(ResponseMessage.PASSWORD_RESET_SUCCESS) : R.fail(ResponseMessage.PASSWORD_RESET_FAIL);
    }


    @Operation(summary = "修改用户状态", description = "启用或禁用用户")
    @PutMapping("/{id}/status")
//    @RequirePermission(dynamic = true)
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        boolean success = userService.updateStatus(id, status);
        return success ? R.ok(ResponseMessage.STATUS_UPDATE_SUCCESS) : R.fail(ResponseMessage.STATUS_UPDATE_FAIL);
    }

    @Operation(summary = "检查用户名", description = "检查用户名是否已存在")
    @GetMapping("/check/username")
//    @RequirePermission(dynamic = true)
    public R<Boolean> checkUsername(@RequestParam String username, @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsUsername(username, excludeId);
        return R.ok(exists);
    }


    @Operation(summary = "检查手机号", description = "检查手机号是否已存在")
    @GetMapping("/check/phone")
//    @RequirePermission(dynamic = true)
    public R<Boolean> checkPhone(@RequestParam String phone, @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsPhone(phone, excludeId);
        return R.ok(exists);
    }


    @Operation(summary = "检查邮箱", description = "检查邮箱是否已存在")
    @GetMapping("/check/email")
//    @RequirePermission(dynamic = true)
    public R<Boolean> checkEmail(@RequestParam String email, @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsEmail(email, excludeId);
        return R.ok(exists);
    }
}