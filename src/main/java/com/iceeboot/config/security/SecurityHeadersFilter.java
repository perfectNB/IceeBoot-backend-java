package com.iceeboot.config.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器
 * 为所有响应添加安全相关的HTTP头，防止点击劫持、MIME嗅探等攻击
 *
 * @author CodeIcee
 * @since 2026-02-12
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 防止页面被嵌入iframe（点击劫持防护）
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 防止浏览器MIME类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // XSS保护（旧版浏览器兼容）
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 限制Referrer信息泄露
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 禁止浏览器缓存敏感响应
        httpResponse.setHeader("Cache-Control", "no-store");
        httpResponse.setHeader("Pragma", "no-cache");

        // 权限策略：禁用不需要的浏览器功能
        httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=()");

        chain.doFilter(request, response);
    }
}
