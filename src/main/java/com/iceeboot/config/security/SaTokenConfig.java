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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

    @Autowired
    private PermissionInterceptor permissionInterceptor;
    
    private java.util.List<String> excludePaths = new java.util.ArrayList<>();
    
    /**
     * 注册Sa-Token拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] currentExcludePaths = excludePaths == null ? new String[0] : excludePaths.toArray(new String[0]);
        // 注册Sa-Token拦截器，校验规则为从请求头获取token并验证
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                    .notMatch(currentExcludePaths)
                    .check(r -> {
                        // 获取当前请求
                        SaRequest request = SaHolder.getRequest();
                        
                        // 打印请求路径用于调试
                        log.debug("Sa-Token拦截器检查请求: {}", request.getUrl());

                        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                            return;
                        }
                        
                        // 从请求头获取token
                        String token = getTokenFromRequest(request);
                        
                        if (!StringUtils.hasText(token)) {
                            throw new cn.dev33.satoken.exception.NotLoginException("未提供访问令牌", "token", "未提供访问令牌");
                        }
                        
                        // 验证token有效性（Sa-Token会自动检查token是否有效和用户是否已登出）
                            try {
                                // 验证token并获取登录ID，如果token无效或用户已登出会抛出异常
                                Object loginId = StpUtil.getLoginIdByToken(token);
                                if (loginId == null) {
                                    throw new cn.dev33.satoken.exception.NotLoginException("无效的访问令牌", "token", "无效的访问令牌");
                                }
                                
                            } catch (cn.dev33.satoken.exception.NotLoginException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new cn.dev33.satoken.exception.NotLoginException("访问令牌验证失败", "token", "访问令牌验证失败");
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
    
    /**
     * 从请求中获取token
     */
    private String getTokenFromRequest(SaRequest request) {
        // 1. 优先从Authorization头获取Bearer token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 去掉"Bearer "前缀
        }
        
        // 2. 从accessToken头获取（Apifox常用）
        String accessToken = request.getHeader("accessToken");
        if (StringUtils.hasText(accessToken)) {
            return accessToken;
        }
        
        // 3. 从Access-Token头获取
        String accessTokenDash = request.getHeader("Access-Token");
        if (StringUtils.hasText(accessTokenDash)) {
            return accessTokenDash;
        }
        
        // 4. 从X-Access-Token头获取
        String xAccessToken = request.getHeader("X-Access-Token");
        if (StringUtils.hasText(xAccessToken)) {
            return xAccessToken;
        }

        return null;
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
