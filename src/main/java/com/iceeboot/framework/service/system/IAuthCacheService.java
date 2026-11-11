/**
 * 认证缓存服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.service.system;

import java.util.List;
import java.util.Map;

/**
 * 认证缓存服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
public interface IAuthCacheService {

    /**
     * 读取用户当前有效菜单，不使用可能滞后的角色资源缓存。
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<Map<String, Object>> getUserMenus(Long userId);

    /**
     * 读取当前有效权限详情，供登录、个人资料等需要完整授权清单的场景使用。
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Map<String, Object>> getUserPermissions(Long userId);

    /** 单次读取当前有效授权，仅校验指定权限；角色或关联撤销后立即生效。 */
    boolean hasUserPermissions(Long userId, List<String> codes, boolean requireAll);

    /**
     * 获取用户角色列表（从缓存或数据库）
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Map<String, Object>> getUserRoles(Long userId);

    /**
     * 获取所有菜单列表（从缓存或数据库）
     *
     * @return 所有菜单列表
     */
    List<Map<String, Object>> getAllMenus();

    /**
     * 获取所有权限列表（从缓存或数据库）
     *
     * @return 所有权限列表
     */
    List<Map<String, Object>> getAllPermissions();

    /**
     * 清除用户相关缓存
     *
     * @param userId 用户ID
     */
    void clearUserCache(Long userId);

    /**
     * 清除所有菜单权限缓存
     */
    void clearAllCache();

    /**
     * 刷新用户缓存
     *
     * @param userId 用户ID
     */
    void refreshUserCache(Long userId);
    
    /**
     * 查询匹配路径的权限编码（跨方法汇总，不用于请求授权）
     *
     * @param url 请求URL
     * @return 权限编码列表
     */
    List<String> getUrlPermissions(String url);

    /** 按 HTTP 方法与 URL 查询权限，支持数据库中的路径变量和通配符。 */
    List<String> getUrlPermissions(String url, String method);
    
    /**
     * 设置URL权限映射
     *
     * @param url 请求URL
     * @param permissions 权限编码列表
     */
    void setUrlPermissions(String url, List<String> permissions);
    
    /**
     * 获取所有URL权限映射
     *
     * @return 以 HTTP 方法与路径模板组成键的权限映射，例如 GET /v1/users/{id}
     */
    Map<String, List<String>> getAllUrlPermissions();
    
    /**
     * 清除URL权限映射缓存
     */
    void clearUrlPermissionCache();
}
