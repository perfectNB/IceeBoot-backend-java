/**
 * 权限拦截器
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.config.interceptor;

import cn.dev33.satoken.exception.NotPermissionException;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.system.IAuthCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 权限拦截器
 * 基于注解的权限验证
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private IAuthCacheService authCacheService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 只处理方法处理器
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        
        // 检查方法上是否有权限注解
        RequirePermission methodAnnotation = method.getAnnotation(RequirePermission.class);
        
        // 检查类上是否有权限注解
        RequirePermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        
        // 如果方法和类都没有权限注解，直接放行
        if (methodAnnotation == null && classAnnotation == null) {
            return true;
        }
        
        // 优先使用方法上的注解，其次使用类上的注解
        RequirePermission annotation = methodAnnotation != null ? methodAnnotation : classAnnotation;
        
        // 获取当前用户ID
        Long userId = TokenUtils.getCurrentUserId();
        if (userId == null) {
            throw new NotPermissionException("用户未登录");
        }
        
        // 获取需要验证的权限编码
        String[] requiredPermissions;
        
        // 检查是否启用动态权限查询
        if (annotation.dynamic()) {
            // 动态权限查询：从Redis中根据URL获取权限编码
            String requestUrl = getRequestUrl(request);
            //获取在数据库中需要有拦截的接口
            List<String> urlPermissions = authCacheService.getUrlPermissions(requestUrl);
            requiredPermissions = urlPermissions.toArray(new String[0]);
        } else {
            // 静态权限验证：使用注解中指定的权限编码
            requiredPermissions = getRequiredPermissions(annotation);
        }
        //此请求接口不存在数据库权限表，则不需要拦截
        if (requiredPermissions.length == 0) {
            return true;
        }
        
        // 获取用户权限
        List<Map<String, Object>> userPermissions = authCacheService.getUserPermissions(userId);
        if (userPermissions == null || userPermissions.isEmpty()) {
            throw new NotPermissionException("用户没有任何权限");
        }
        
        // 提取用户权限编码
        List<String> userPermissionCodes = userPermissions.stream()
                .map(permission -> (String) permission.get("code"))
                .filter(code -> code != null && !code.isEmpty())
                .toList();
        
        // 验证权限
        boolean hasPermission = checkPermission(userPermissionCodes, requiredPermissions, annotation.requireAll());
        
        if (!hasPermission) {
            String permissionDesc = annotation.description();
            if (!StringUtils.hasText(permissionDesc)) {
                permissionDesc = String.join(",", requiredPermissions);
            }
            throw new NotPermissionException("缺少权限: " + permissionDesc);
        }
        
        return true;
    }
    
    /**
     * 获取需要验证的权限编码
     */
    private String[] getRequiredPermissions(RequirePermission annotation) {
        // 优先使用value数组
        if (annotation.value().length > 0) {
            return annotation.value();
        }
        
        // 其次使用code单个值
        if (StringUtils.hasText(annotation.code())) {
            return new String[]{annotation.code()};
        }
        
        return new String[0];
    }
    
    /**
     * 获取请求URL
     * 
     * @param request HTTP请求
     * @return 请求URL
     */
    private String getRequestUrl(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();


//        // 移除上下文路径
        if (StringUtils.hasText(contextPath) && requestURI.startsWith(contextPath)) {
            requestURI = requestURI.substring(contextPath.length());
        }
        
        return requestURI;
    }
    
    /**
     * 检查权限
     * 
     * @param userPermissions 用户权限编码列表
     * @param requiredPermissions 需要的权限编码数组
     * @param requireAll 是否需要所有权限
     * @return 是否有权限
     */
    private boolean checkPermission(List<String> userPermissions, String[] requiredPermissions, boolean requireAll) {
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }
        
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }
        
        if (requireAll) {
            // 需要所有权限都满足（AND）
            return Arrays.stream(requiredPermissions)
                    .allMatch(userPermissions::contains);
        } else {
            // 满足任意一个权限即可（OR）
            return Arrays.stream(requiredPermissions)
                    .anyMatch(userPermissions::contains);
        }
    }
}
