/**
 * Sa-Token配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.config.security;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.iceeboot.config.interceptor.PermissionInterceptor;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.common.exception.AuthServiceUnavailableException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token配置类
 * @author CodeIcee
 * @date 2025-08-11
 */
@Configuration
@ConfigurationProperties(prefix = "iceeboot.auth")
@Data
@Slf4j
public class SaTokenConfig implements WebMvcConfigurer {

    private static final String EVENT_STREAM = "text/event-stream";
    private static final String SSE_AUTHENTICATED = SaTokenConfig.class.getName() + ".authenticated";

    private static final List<String> DOC_EXCLUDE_PATHS = List.of(
            "/doc.html",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            "/iceeboot/api/doc.html",
            "/iceeboot/api/swagger-ui.html",
            "/iceeboot/api/swagger-ui/**",
            "/iceeboot/api/v3/api-docs",
            "/iceeboot/api/v3/api-docs/**",
            "/iceeboot/api/webjars/**"
    );

    @Autowired
    private PermissionInterceptor permissionInterceptor;
    
    private java.util.List<String> excludePaths = new java.util.ArrayList<>();

    /**
     * 仅在开发/测试环境显式开启匿名文档访问
     */
    private boolean anonymousDocsEnabled = false;
    
    /**
     * 注册Sa-Token拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] currentExcludePaths = buildExcludePaths();
        // 注册Sa-Token拦截器，校验规则为从请求头获取token并验证
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                    .notMatch(currentExcludePaths)
                    .check(r -> {
                        // 获取当前请求
                        SaRequest request = SaHolder.getRequest();
                        HttpServletRequest servletRequest = currentHttpRequest();
                        
                        // 打印请求路径用于调试
                        log.debug("Sa-Token拦截器检查请求: {}", request.getUrl());

                        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                            return;
                        }

                        // SSE 请求在首轮 REQUEST 阶段已经通过鉴权后，
                        // 后续 ASYNC/ERROR 派发不应再次验 token。
                        // 否则一旦期间发生 token 刷新，旧流会在异步派发时被误判为“无效访问令牌”而中断。
                        if (shouldSkipAsyncSseAuth(servletRequest)) {
                            return;
                        }
                        
                        String token = TokenUtils.resolveRequestToken(request);
                        
                        if (!StringUtils.hasText(token)) {
                            throw new cn.dev33.satoken.exception.NotLoginException("未提供访问令牌", "token", "未提供访问令牌");
                        }
                        
                        // Only a definitive invalid credential is a 401. A storage outage
                        // must reject this request without making clients discard valid tokens.
                        try {
                            Object loginId = StpUtil.getLoginIdByToken(token);
                            if (loginId == null) {
                                throw new cn.dev33.satoken.exception.NotLoginException("无效的访问令牌", "token", "无效的访问令牌");
                            }
                            if (servletRequest != null) servletRequest.setAttribute(SSE_AUTHENTICATED, Boolean.TRUE);
                        } catch (cn.dev33.satoken.exception.NotLoginException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new AuthServiceUnavailableException(e);
                        }
                        
                        // 检查权限（后续实现动态权限校验）
                        // checkPermission(request);
                    });
        })).addPathPatterns("/**");
        
        // 注册权限拦截器（基于注解的权限验证）
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(currentExcludePaths);
    }

    private String[] buildExcludePaths() {
        List<String> paths = new ArrayList<>();
        if (excludePaths != null) {
            paths.addAll(excludePaths);
        }
        if (anonymousDocsEnabled) {
            paths.addAll(DOC_EXCLUDE_PATHS);
        }
        return paths.toArray(new String[0]);
    }

    private HttpServletRequest currentHttpRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private boolean shouldSkipAsyncSseAuth(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        DispatcherType dispatcherType = request.getDispatcherType();
        if (dispatcherType != DispatcherType.ASYNC && dispatcherType != DispatcherType.ERROR) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return Boolean.TRUE.equals(request.getAttribute(SSE_AUTHENTICATED))
                && accept != null && accept.contains(EVENT_STREAM);
    }
    
    /**
     * 检查权限（动态权限校验）
     * TODO: 实现动态权限校验逻辑
     */
    private void checkPermission(Object request) {
        // 获取当前请求的URI
        // String requestURI = ((HttpServletRequest) request).getRequestURI();
        
        // 获取当前用户的权限列表
        // List<String> permissions = getUserPermissions(StpUtil.getLoginIdAsString());
        
        // 检查用户是否有访问该接口的权限
        // if (!hasPermission(requestURI, permissions)) {
        //     throw new NotPermissionException("没有访问权限");
        // }
    }
}
