/**
 * 认证控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.system.LoginDTO;
import com.zboot.framework.service.system.IAuthService;
import com.zboot.framework.vo.system.LoginVO;
import com.zboot.framework.vo.system.SysUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "认证管理", description = "用户登录、登出、获取用户信息等认证相关接口")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {


    private final IAuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户名密码登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return R.ok(loginVO);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/user")
    public R<SysUserVO> getCurrentUser() {
        SysUserVO userVO = authService.getCurrentUser();
        return R.ok(userVO);
    }

    /**
     * 刷新令牌
     */
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestParam String refreshToken) {
        LoginVO loginVO = authService.refresh(refreshToken);
        return R.ok(loginVO);
    }

    /**
     * 微信登录
     */
    @Operation(summary = "微信登录", description = "使用微信授权码登录")
    @PostMapping("/wechat")
    public R<LoginVO> wechatLogin(@RequestParam String code) {
        LoginVO loginVO = authService.wechatLogin(code);
        return R.ok(loginVO);
    }

    /**
     * 检查登录状态
     */
    @Operation(summary = "检查登录状态", description = "检查当前用户是否已登录")
    @GetMapping("/check")
    public R<Boolean> checkLogin() {
        boolean isLogin = StpUtil.isLogin();
        return R.ok(isLogin);
    }
}