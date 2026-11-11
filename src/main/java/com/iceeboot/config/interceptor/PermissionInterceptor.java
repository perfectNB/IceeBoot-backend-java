/**
 * 权限拦截器
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.config.interceptor;

import cn.dev33.satoken.exception.NotPermissionException;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.system.IAuthCacheService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 权限拦截器
 * 基于注解的权限验证
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String EVENT_STREAM = "text/event-stream";
    private static final String SSE_AUTHORIZED = PermissionInterceptor.class.getName() + ".authorized";

    private final IAuthCacheService authCacheService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // SSE 请求在首轮鉴权通过后，后续 ASYNC/ERROR 派发只负责把流继续写完。
        // 这里跳过二次权限校验，避免 token 轮换时把已建立的流误切断。
        if (shouldSkipAsyncSsePermissionCheck(request)) {
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
            List<String> urlPermissions = authCacheService.getUrlPermissions(requestUrl, request.getMethod());
            requiredPermissions = urlPermissions == null ? new String[0] : urlPermissions.toArray(new String[0]);
        } else {
            // 静态权限验证：使用注解中指定的权限编码
            requiredPermissions = getRequiredPermissions(annotation);
        }
        requiredPermissions = Arrays.stream(requiredPermissions)
                .filter(StringUtils::hasText)
                .distinct()
                .toArray(String[]::new);
        // 已声明权限校验的接口必须配置有效权限，缺失配置时禁止放行。
        if (requiredPermissions.length == 0) {
            throw new NotPermissionException(ResponseMessage.Permission.PERMISSION_CONFIG_MISSING);
        }
        
        // 仅查询本次所需权限码，避免每次请求传输用户的全部权限详情。
        boolean hasPermission = authCacheService.hasUserPermissions(
                userId, Arrays.asList(requiredPermissions), annotation.requireAll());
        
        if (!hasPermission) {
            String permissionDesc = annotation.description();
            if (!StringUtils.hasText(permissionDesc)) {
                permissionDesc = String.join(",", requiredPermissions);
            }
            throw new NotPermissionException("缺少权限: " + permissionDesc);
        }
        
        request.setAttribute(SSE_AUTHORIZED, Boolean.TRUE);
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
    
    private boolean shouldSkipAsyncSsePermissionCheck(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        DispatcherType dispatcherType = request.getDispatcherType();
        if (dispatcherType != DispatcherType.ASYNC && dispatcherType != DispatcherType.ERROR) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return Boolean.TRUE.equals(request.getAttribute(SSE_AUTHORIZED))
                && accept != null && accept.contains(EVENT_STREAM);
    }
}
