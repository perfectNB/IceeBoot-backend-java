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
     * 获取用户菜单列表（从缓存或数据库）
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<Map<String, Object>> getUserMenus(Long userId);

    /**
     * 获取用户权限列表（从缓存或数据库）
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Map<String, Object>> getUserPermissions(Long userId);

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
     * 获取URL权限映射
     *
     * @param url 请求URL
     * @return 权限编码列表
     */
    List<String> getUrlPermissions(String url);
    
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
     * @return URL权限映射Map
     */
    Map<String, List<String>> getAllUrlPermissions();
    
    /**
     * 清除URL权限映射缓存
     */
    void clearUrlPermissionCache();
}