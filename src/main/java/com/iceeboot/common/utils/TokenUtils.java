package com.iceeboot.common.utils;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.stp.StpUtil;
import com.iceeboot.common.exception.IceeBootException;
import org.springframework.util.StringUtils;

/**
 * Token工具类
 * 提供token获取、用户信息获取等功能
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
public class TokenUtils {

    /**
     * 从当前请求中获取token
     * 
     * @return token值，如果没有找到则返回null
     */
    public static String getToken() {
        try {
            SaRequest request = SaHolder.getRequest();
            return resolveRequestToken(request);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从请求中获取token
     * 
     * @param request 请求对象
     * @return token值
     */
    public static String resolveRequestToken(SaRequest request) {
        if (request == null) {
            return null;
        }
        
        // 1. 优先从Authorization头获取Bearer token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
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
        
        // 5. 从 Sa-Token 上下文获取（支持 Cookie，不从 URL 参数读取凭据）。
        String token = StpUtil.getTokenValue();
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        return null;
    }
    
    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        try {
            String token = getToken();
            if (token == null) {
                return null;
            }
            
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId != null) {
                return Long.valueOf(loginId.toString());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查当前登录用户ID是否和查询资源的创建人id相同
     *
     * @return 用户ID，如果未登录则返回null
     */
    public static Boolean checkUserId(Long objectId) {
        String token = getToken();
        if (token == null) {
            throw new IceeBootException("不存在token");
        }

        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId != null) {
            Long userId = Long.valueOf(loginId.toString());
            if (objectId!=userId){
                throw new IceeBootException("这不属于您的资源");
            }
        }
        return true;
    }
    
    /**
     * 获取当前登录用户ID（字符串格式）
     * 
     * @return 用户ID字符串，如果未登录则返回null
     */
    public static String getCurrentUserIdAsString() {
        try {
            Long userId = getCurrentUserId();
            return userId != null ? userId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 检查当前用户是否已登录
     * 
     * @return true-已登录，false-未登录
     */
    public static boolean isLogin() {
        try {
            String token = getToken();
            if (token == null) {
                return false;
            }
            
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查指定token是否有效
     * 
     * @param token 要检查的token
     * @return true-有效，false-无效
     */
    public static boolean isValidToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }
            
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 根据token获取用户ID
     * 
     * @param token 访问令牌
     * @return 用户ID，如果token无效则返回null
     */
    public static Long getUserIdByToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }
            
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId != null) {
                return Long.valueOf(loginId.toString());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 登出指定token
     * 
     * @param token 要登出的token
     */
    public static void logoutByToken(String token) {
        try {
            if (token != null && !token.isEmpty()) {
                StpUtil.logoutByTokenValue(token);
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }
    
    /**
     * 登出当前用户
     */
    public static void logout() {
        try {
            String token = getToken();
            if (token != null) {
                logoutByToken(token);
            } else {
                StpUtil.logout();
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }
}