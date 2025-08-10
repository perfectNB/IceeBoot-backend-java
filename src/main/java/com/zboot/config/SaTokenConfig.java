/**
 * Sa-Token配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    
    /**
     * 注册Sa-Token拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器，校验规则为StpUtil.checkLogin()登录校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                    .notMatch("/zboot/auth/login")
                    .notMatch("/zboot/auth/register")
                    .notMatch("/zboot/auth/wechat/login")
                    .notMatch("/zboot/auth/captcha")
                    .notMatch("/zboot/health")
                    .notMatch("/zboot/doc.html")
                    .notMatch("/zboot/swagger-ui/**")
                    .notMatch("/zboot/v3/api-docs/**")
                    .notMatch("/zboot/webjars/**")
                    .notMatch("/zboot/favicon.ico")
                    .notMatch("/zboot/error")
                    .check(r -> {
                        // 检查登录状态
                        StpUtil.checkLogin();
                        
                        // 检查权限（后续实现动态权限校验）
                        // checkPermission(r);
                    });
        })).addPathPatterns("/**");
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