package com.iceeboot.framework.controller.system;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.system.LoginDTO;
import com.iceeboot.framework.service.system.IAuthService;
import com.iceeboot.framework.vo.system.LoginVO;
import com.iceeboot.framework.vo.system.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Map;

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


    @Operation(summary = "test1", description = "test")
    @GetMapping("/test1")
    @RequirePermission(dynamic = true)
    public R<LoginVO> test1() {

        return R.ok(ResponseMessage.Common.SYSTEM_WELCOME);
    }

    @Operation(summary = "test2", description = "test")
    @GetMapping("/test2")
    @RequirePermission(dynamic = true)
    public R<LoginVO> test2() {

        return R.ok("谢谢您使用iceeboot超级脚手架");
    }


    @Operation(summary = "用户登录", description = "用户名密码登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return R.ok(loginVO);
    }


    @Operation(summary = "用户登出", description = "退出登录")
    @PostMapping("/logout")
    @RequirePermission(dynamic = true)
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }


    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/user")
    @RequirePermission(dynamic = true)
    public R<SysUserVO> getCurrentUser() {
        SysUserVO userVO = authService.getCurrentUser();
        return R.ok(userVO);
    }


    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestParam(name = "refreshToken", required = false) String refreshToken,
                              @RequestBody(required = false) Map<String, Object> body) {
        String token = refreshToken;
        if (!StringUtils.hasText(token) && body != null) {
            Object value = body.get("refreshToken");
            if (value != null) {
                token = value.toString();
            }
        }
        LoginVO loginVO = authService.refresh(token);
        return R.ok(loginVO);
    }

    @Operation(summary = "获取微信扫码登录授权URL", description = "返回微信OAuth授权URL，前端使用该URL生成二维码或跳转")
    @GetMapping("/wechat/auth-url")
    public R<String> getWechatAuthUrl() {
        String authUrl = authService.getWechatAuthUrl();
        return R.ok(authUrl);
    }

    @Operation(summary = "微信OAuth回调接口", description = "由微信服务器调用，处理授权码并重定向到前端")
    @GetMapping("/wechat/callback")
    public String handleWechatCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state) throws UnsupportedEncodingException {
        String redirectUrl = authService.handleWechatCallback(code, state);
        return "redirect:" + redirectUrl;
    }

    @Operation(summary = "使用ticket换取登录信息", description = "前端使用ticket换取accessToken和用户信息")
    @PostMapping("/wechat/exchange-ticket")
    public R<LoginVO> exchangeTicket(@RequestParam(name = "ticket") String ticket) {
        LoginVO loginVO = authService.exchangeTicket(ticket);
        return R.ok(loginVO);
    }

    @Operation(summary = "检查登录状态", description = "检查当前用户是否已登录")
    @GetMapping("/check")
    @RequirePermission(dynamic = true)
    public R<Boolean> checkLogin() {
        boolean isLogin = authService.checkLogin();
        return R.ok(isLogin);
    }


}
