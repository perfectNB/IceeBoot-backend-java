package com.iceeboot.framework.controller.system;

import com.iceeboot.common.annotation.RateLimit;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.RateLimitType;
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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;
import com.iceeboot.framework.dto.create.system.WechatTicketDTO;
import jakarta.validation.Valid;
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

    @Value("${YunGouOS.frontendUrl}")
    private String frontendUrl;


    @Operation(summary = "用户登录", description = "用户名密码登录")
    @PostMapping("/login")
    @RateLimit(count = 5, time = 300, limitType = RateLimitType.IP, message = "登录尝试过于频繁，请5分钟后再试")
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
    @RateLimit(count = 10, time = 60, limitType = RateLimitType.IP, message = "刷新令牌过于频繁，请稍后再试")
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


    @Operation(summary = "获取微信扫码登录二维码", description = "返回微信二维码")
    @GetMapping({"/wechat/qr", "/wechat/innerQrCodeInfo-url"})
    public R<Object> getWechatQr() {
        return R.ok(authService.getWechatQr());
    }

    @Operation(summary = "微信OAuth回调接口", description = "由微信服务器调用，处理授权码并重定向到前端")
    @GetMapping("/wechat/callback")
    public void handleWechatCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            HttpServletResponse response) throws IOException {
        try {
            String redirectUrl = authService.handleWechatCallback(code, state);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            String fallbackUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .replaceQueryParam("error", "微信回调处理失败").build().encode().toUriString();
            response.sendRedirect(fallbackUrl);
        }
    }

    @Operation(summary = "使用ticket换取登录信息", description = "前端使用ticket换取accessToken和用户信息")
    @PostMapping("/wechat/exchange-ticket")
    @RateLimit(count = 10, time = 60, limitType = RateLimitType.IP, message = "请求过于频繁，请稍后再试")
    public R<LoginVO> exchangeTicket(@RequestParam(name = "ticket", required = false) String ticket,
                                     @Valid @RequestBody(required = false) WechatTicketDTO body) {
        String credential = body == null ? ticket : body.ticket();
        LoginVO loginVO = authService.exchangeTicket(credential);
        return R.ok(loginVO);
    }

    @Operation(summary = "检查登录状态", description = "检查当前用户是否已登录")
    @GetMapping("/wechat/check/{code}")
    public R<Map<String, Object>> checkLogin(@PathVariable("code") String code) {
        return R.ok(authService.checkLogin(code));
    }


}
