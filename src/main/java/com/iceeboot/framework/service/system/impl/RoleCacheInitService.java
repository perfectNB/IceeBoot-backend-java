/**
 * 角色缓存初始化服务
 * 项目启动时自动加载角色权限和菜单到缓存
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.framework.entity.system.*;
import com.iceeboot.framework.service.system.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色缓存初始化服务
 * 项目启动时自动加载角色权限和菜单到缓存
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Slf4j
@Service
public class RoleCacheInitService implements ApplicationRunner {

    @Autowired
    private ISysRoleService roleService;
    
    @Autowired
    private ISysRolePermissionService rolePermissionService;
    
    @Autowired
    private ISysRoleMenuService roleMenuService;
    
    @Autowired
    private ISysPermissionService permissionService;
    
    @Autowired
    private ISysMenuService menuService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("角色缓存初始化服务启动，开始加载角色权限和菜单缓存...");
        
        try {
            // 初始化角色权限和菜单缓存
            initRoleCache();
            
            log.info("角色缓存初始化完成");
        } catch (Exception e) {
            log.error("角色缓存初始化失败", e);
        }
    }
    
    /**
     * 初始化角色缓存
     */
    private void initRoleCache() {
        // 1. 获取所有激活且未删除的角色
        List<RoleDO> activeRoles = roleService.list(
            new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getStatus, SystemConstants.Status.ACTIVE)
                .eq(RoleDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                .orderByAsc(RoleDO::getSort)
        );
        
        log.info("找到 {} 个激活的角色，开始初始化缓存", activeRoles.size());
        
        // 2. 遍历每个角色，加载其权限和菜单
        for (RoleDO role : activeRoles) {
            try {
                // 初始化角色权限缓存
                initRolePermissionCache(role.getId());
                
                // 初始化角色菜单缓存
                initRoleMenuCache(role.getId());
                
                log.debug("角色 [{}] 缓存初始化完成", role.getName());
            } catch (Exception e) {
                log.error("角色 [{}] 缓存初始化失败: {}", role.getName(), e.getMessage());
            }
        }
    }
    
    /**
     * 初始化角色权限缓存
     * 
     * @param roleId 角色ID
     */
    private void initRolePermissionCache(Long roleId) {
        // 获取角色的权限ID列表
        List<Long> permissionIds = rolePermissionService.list(
            new LambdaQueryWrapper<RolePermissionDO>()
                .eq(RolePermissionDO::getRoleId, roleId)
                .eq(RolePermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
        ).stream().map(RolePermissionDO::getPermissionId).collect(Collectors.toList());
        
        if (!permissionIds.isEmpty()) {
            // 获取权限详细信息
            List<PermissionDO> permissions = permissionService.list(
                new LambdaQueryWrapper<PermissionDO>()
                    .in(PermissionDO::getId, permissionIds)
                    .eq(PermissionDO::getStatus, SystemConstants.Status.ACTIVE)
                    .eq(PermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                    .orderByAsc(PermissionDO::getSort)
            );
            
            // 存储完整的权限对象列表
            String cacheKey = CacheConstants.Auth.ROLE_PERMISSIONS_KEY + roleId;
            redisTemplate.opsForValue().set(
                cacheKey, 
                permissions
            );
            
            log.debug("角色 [{}] 权限缓存已更新，共 {} 个权限", roleId, permissions.size());
        } else {
            // 如果没有权限，存储具体类型的空列表
            String cacheKey = CacheConstants.Auth.ROLE_PERMISSIONS_KEY + roleId;
            List<PermissionDO> emptyPermissions = new ArrayList<>();
            redisTemplate.opsForValue().set(
                cacheKey, 
                emptyPermissions
            );
            
            log.debug("角色 [{}] 没有分配权限，已缓存空列表", roleId);
        }
    }
    
    /**
     * 初始化角色菜单缓存
     * 
     * @param roleId 角色ID
     */
    private void initRoleMenuCache(Long roleId) {
        // 获取角色的菜单ID列表
        List<Long> menuIds = roleMenuService.list(
            new LambdaQueryWrapper<RoleMenuDO>()
                .eq(RoleMenuDO::getRoleId, roleId)
                .eq(RoleMenuDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
        ).stream().map(RoleMenuDO::getMenuId).collect(Collectors.toList());
        
        if (!menuIds.isEmpty()) {
            // 获取菜单详细信息
            List<MenuDO> menus = menuService.list(
                new LambdaQueryWrapper<MenuDO>()
                    .in(MenuDO::getId, menuIds)
                    .eq(MenuDO::getStatus, SystemConstants.Status.ACTIVE)
                    .eq(MenuDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                    .orderByAsc(MenuDO::getSort)
            );
            
            // 存储完整的菜单对象列表
            String cacheKey = CacheConstants.Auth.ROLE_MENUS_KEY + roleId;
            redisTemplate.opsForValue().set(
                cacheKey, 
                menus
            );
            
            log.debug("角色 [{}] 菜单缓存已更新，共 {} 个菜单", roleId, menus.size());
        } else {
            // 如果没有菜单，存储具体类型的空列表
            String cacheKey = CacheConstants.Auth.ROLE_MENUS_KEY + roleId;
            List<MenuDO> emptyMenus = new ArrayList<>();
            redisTemplate.opsForValue().set(
                cacheKey, 
                emptyMenus
            );
            
            log.debug("角色 [{}] 没有分配菜单，已缓存空列表", roleId);
        }
    }
    
    /**
     * 手动刷新角色缓存
     * 可以在角色权限变更时调用
     */
    public void refreshRoleCache() {
        log.info("开始手动刷新角色缓存...");
        initRoleCache();
        log.info("角色缓存刷新完成");
    }
    
    /**
     * 刷新指定角色的缓存
     * 
     * @param roleId 角色ID
     */
    public void refreshRoleCache(Long roleId) {
        log.info("开始刷新角色 [{}] 的缓存...", roleId);
        
        try {
            // 刷新角色权限缓存
            initRolePermissionCache(roleId);
            
            // 刷新角色菜单缓存
            initRoleMenuCache(roleId);
            
            log.info("角色 [{}] 缓存刷新完成", roleId);
        } catch (Exception e) {
            log.error("角色 [{}] 缓存刷新失败: {}", roleId, e.getMessage());
        }
    }
    
    /**
     * 清除指定角色的缓存
     * 
     * @param roleId 角色ID
     */
    public void clearRoleCache(Long roleId) {
        String permissionCacheKey = CacheConstants.Auth.ROLE_PERMISSIONS_KEY + roleId;
        String menuCacheKey = CacheConstants.Auth.ROLE_MENUS_KEY + roleId;
        
        redisTemplate.delete(permissionCacheKey);
        redisTemplate.delete(menuCacheKey);
        
        log.info("角色 [{}] 缓存已清除", roleId);
    }
}