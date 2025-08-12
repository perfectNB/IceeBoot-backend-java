package com.zboot.project.zboot_test.controller.user;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.project.zboot_test.entity.user.UserDO;
import com.zboot.project.zboot_test.dto.user.UserDTO;
import com.zboot.project.zboot_test.query.user.UserQuery;
import com.zboot.project.zboot_test.service.user.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User管理", description = "User管理相关接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "分页查询User列表")
    @GetMapping("/page")
    public R<PageResult<UserDO>> getUserPage(UserQuery query) {
        PageResult<UserDO> pageResult = userService.getUserPage(query);
        return R.ok(pageResult);
    }

    @Operation(summary = "根据ID查询User详情")
    @GetMapping("/{id}")
    public R<UserDO> getUserById(@PathVariable Long id) {
        UserDO user = userService.getById(id);
        return R.ok(user);
    }

    @Operation(summary = "创建User")
    @PostMapping
    public R<UserDO> createUser(@RequestBody UserDTO userDTO) {
        UserDO result = userService.createUser(userDTO);
        return R.ok(result);
    }

    @Operation(summary = "更新User")
    @PutMapping
    public R<UserDO> updateUser(@RequestBody UserDTO userDTO) {
        UserDO result = userService.updateUser(userDTO);
        return R.ok(result);
    }

    @Operation(summary = "删除User")
    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}