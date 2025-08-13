/**
 * 认证缓存服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zboot.framework.entity.system.*;
import com.zboot.framework.mapper.system.*;
import com.zboot.framework.service.system.IAuthCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证缓存服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Service
public class AuthCacheServiceImpl implements IAuthCacheService {

    private static final String USER_MENUS_KEY = "auth:user:menus:";
    private static final String USER_PERMISSIONS_KEY = "auth:user:permissions:";
    private static final String USER_ROLES_KEY = "auth:user:roles:";
    private static final String ALL_MENUS_KEY = "auth:all:menus";
    private static final String ALL_PERMISSIONS_KEY = "auth:all:permissions";
    private static final String ALL_ROLES_KEY = "auth:all:roles";
    private static final String ALL_USER_ROLES_KEY = "auth:all:user_roles";
    private static final String ALL_ROLE_MENUS_KEY = "auth:all:role_menus";
    private static final String ALL_ROLE_PERMISSIONS_KEY = "auth:all:role_permissions";
    private static final long CACHE_EXPIRE_TIME = 30; // 30分钟

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserMenus(Long userId) {
        String key = USER_MENUS_KEY + userId;
        List<Map<String, Object>> menus = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
        
        if (menus == null) {
            menus = loadUserMenusFromDB(userId);
            redisTemplate.opsForValue().set(key, menus, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return menus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserPermissions(Long userId) {
        String key = USER_PERMISSIONS_KEY + userId;
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
        
        if (permissions == null) {
            permissions = loadUserPermissionsFromDB(userId);
            redisTemplate.opsForValue().set(key, permissions, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return permissions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserRoles(Long userId) {
        String key = USER_ROLES_KEY + userId;
        List<Map<String, Object>> roles = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
        
        if (roles == null) {
            roles = loadUserRolesFromDB(userId);
            redisTemplate.opsForValue().set(key, roles, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return roles;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllMenus() {
        List<Map<String, Object>> menus = (List<Map<String, Object>>) redisTemplate.opsForValue().get(ALL_MENUS_KEY);
        
        if (menus == null) {
            menus = loadAllMenusFromDB();
            redisTemplate.opsForValue().set(ALL_MENUS_KEY, menus, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return menus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllPermissions() {
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) redisTemplate.opsForValue().get(ALL_PERMISSIONS_KEY);
        
        if (permissions == null) {
            permissions = loadAllPermissionsFromDB();
            redisTemplate.opsForValue().set(ALL_PERMISSIONS_KEY, permissions, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return permissions;
    }

    /**
     * 获取所有角色列表（从缓存或数据库）
     *
     * @return 所有角色列表
     */
    @SuppressWarnings("unchecked")
    public List<RoleDO> getAllRoles() {
        List<RoleDO> roles = (List<RoleDO>) redisTemplate.opsForValue().get(ALL_ROLES_KEY);
        
        if (roles == null) {
            roles = loadAllRolesFromDB();
            redisTemplate.opsForValue().set(ALL_ROLES_KEY, roles, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return roles != null ? roles : new ArrayList<>();
    }

    /**
     * 获取所有用户角色关系（从缓存或数据库）
     *
     * @return 所有用户角色关系
     */
    @SuppressWarnings("unchecked")
    public List<UserRoleDO> getAllUserRoles() {
        List<UserRoleDO> userRoles = (List<UserRoleDO>) redisTemplate.opsForValue().get(ALL_USER_ROLES_KEY);
        
        if (userRoles == null) {
            userRoles = loadAllUserRolesFromDB();
            redisTemplate.opsForValue().set(ALL_USER_ROLES_KEY, userRoles, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return userRoles != null ? userRoles : new ArrayList<>();
    }

    /**
     * 获取所有角色菜单关系（从缓存或数据库）
     *
     * @return 所有角色菜单关系
     */
    @SuppressWarnings("unchecked")
    public List<RoleMenuDO> getAllRoleMenus() {
        List<RoleMenuDO> roleMenus = (List<RoleMenuDO>) redisTemplate.opsForValue().get(ALL_ROLE_MENUS_KEY);
        
        if (roleMenus == null) {
            roleMenus = loadAllRoleMenusFromDB();
            redisTemplate.opsForValue().set(ALL_ROLE_MENUS_KEY, roleMenus, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return roleMenus != null ? roleMenus : new ArrayList<>();
    }

    /**
     * 获取所有角色权限关系（从缓存或数据库）
     *
     * @return 所有角色权限关系
     */
    @SuppressWarnings("unchecked")
    public List<RolePermissionDO> getAllRolePermissions() {
        List<RolePermissionDO> rolePermissions = (List<RolePermissionDO>) redisTemplate.opsForValue().get(ALL_ROLE_PERMISSIONS_KEY);
        
        if (rolePermissions == null) {
            rolePermissions = loadAllRolePermissionsFromDB();
            redisTemplate.opsForValue().set(ALL_ROLE_PERMISSIONS_KEY, rolePermissions, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return rolePermissions != null ? rolePermissions : new ArrayList<>();
    }

    @Override
    public void clearUserCache(Long userId) {
        redisTemplate.delete(USER_MENUS_KEY + userId);
        redisTemplate.delete(USER_PERMISSIONS_KEY + userId);
        redisTemplate.delete(USER_ROLES_KEY + userId);
    }

    @Override
    public void clearAllCache() {
        // 清除全量数据缓存
        redisTemplate.delete(ALL_MENUS_KEY);
        redisTemplate.delete(ALL_PERMISSIONS_KEY);
        redisTemplate.delete(ALL_ROLES_KEY);
        redisTemplate.delete(ALL_USER_ROLES_KEY);
        redisTemplate.delete(ALL_ROLE_MENUS_KEY);
        redisTemplate.delete(ALL_ROLE_PERMISSIONS_KEY);
        
        // 清除所有用户缓存
        Set<String> userMenuKeys = redisTemplate.keys(USER_MENUS_KEY + "*");
        Set<String> userPermissionKeys = redisTemplate.keys(USER_PERMISSIONS_KEY + "*");
        Set<String> userRoleKeys = redisTemplate.keys(USER_ROLES_KEY + "*");
        
        if (userMenuKeys != null && !userMenuKeys.isEmpty()) {
            redisTemplate.delete(userMenuKeys);
        }
        if (userPermissionKeys != null && !userPermissionKeys.isEmpty()) {
            redisTemplate.delete(userPermissionKeys);
        }
        if (userRoleKeys != null && !userRoleKeys.isEmpty()) {
            redisTemplate.delete(userRoleKeys);
        }
    }

    @Override
    public void refreshUserCache(Long userId) {
        clearUserCache(userId);
        getUserMenus(userId);
        getUserPermissions(userId);
        getUserRoles(userId);
    }

    /**
     * 从数据库加载用户菜单（优化版本，使用缓存数据）
     */
    private List<Map<String, Object>> loadUserMenusFromDB(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // 获取用户角色
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取角色对应的菜单ID
        List<Long> menuIds = getRoleMenuIds(roleIds);
        if (menuIds == null || menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 从缓存的全量菜单中过滤出用户菜单
        List<Map<String, Object>> allMenus = getAllMenus();
        if (allMenus == null || allMenus.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<MenuDO> userMenus = new ArrayList<>();
        for (Map<String, Object> menuMap : allMenus) {
            Object idObj = menuMap.get("id");
            if (idObj != null && menuIds.contains(Long.valueOf(idObj.toString()))) {
                // 这里需要从数据库获取完整的MenuDO对象来构建树形结构
                MenuDO menu = menuMapper.selectById(Long.valueOf(idObj.toString()));
                if (menu != null) {
                    userMenus.add(menu);
                }
            }
        }
        
        return buildMenuTree(userMenus);
    }

    /**
     * 从数据库加载用户权限（优化版本，使用缓存数据）
     */
    private List<Map<String, Object>> loadUserPermissionsFromDB(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // 获取用户角色
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取角色对应的权限ID
        List<Long> permissionIds = getRolePermissionIds(roleIds);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 从缓存的全量权限中过滤出用户权限
        List<Map<String, Object>> allPermissions = getAllPermissions();
        if (allPermissions == null || allPermissions.isEmpty()) {
            return new ArrayList<>();
        }
        
        return allPermissions.stream()
                .filter(permissionMap -> {
                    Object idObj = permissionMap.get("id");
                    if (idObj == null) {
                        return false;
                    }
                    try {
                        Long permissionId = Long.valueOf(idObj.toString());
                        return permissionIds.contains(permissionId);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 从数据库加载用户角色（优化版本，使用缓存数据）
     */
    private List<Map<String, Object>> loadUserRolesFromDB(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 从缓存的全量角色中过滤出用户角色
        List<RoleDO> allRoles = getAllRoles();
        if (allRoles == null || allRoles.isEmpty()) {
            return new ArrayList<>();
        }
        
        return allRoles.stream()
                .filter(role -> role != null && role.getId() != null && roleIds.contains(role.getId()))
                .map(this::convertRoleToMap)
                .collect(Collectors.toList());
    }

    /**
     * 从数据库加载所有菜单
     */
    private List<Map<String, Object>> loadAllMenusFromDB() {
        List<MenuDO> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getDelFlag, 0)
                .orderByAsc(MenuDO::getSort));
        return buildMenuTree(menus);
    }

    /**
     * 从数据库加载所有权限
     */
    private List<Map<String, Object>> loadAllPermissionsFromDB() {
        List<PermissionDO> permissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionDO>()
                .eq(PermissionDO::getDelFlag, 0));
        return permissions.stream().map(this::convertPermissionToMap).collect(Collectors.toList());
    }

    /**
     * 从数据库加载所有角色
     */
    private List<RoleDO> loadAllRolesFromDB() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getDelFlag, 0));
    }

    /**
     * 从数据库加载所有用户角色关系
     */
    private List<UserRoleDO> loadAllUserRolesFromDB() {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getDelFlag, 0));
    }

    /**
     * 从数据库加载所有角色菜单关系
     */
    private List<RoleMenuDO> loadAllRoleMenusFromDB() {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenuDO>()
                .eq(RoleMenuDO::getDelFlag, 0));
    }

    /**
     * 从数据库加载所有角色权限关系
     */
    private List<RolePermissionDO> loadAllRolePermissionsFromDB() {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermissionDO>()
                .eq(RolePermissionDO::getDelFlag, 0));
    }

    /**
     * 获取用户角色ID列表（从缓存中过滤）
     */
    private List<Long> getUserRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        List<UserRoleDO> allUserRoles = getAllUserRoles();
        if (allUserRoles == null || allUserRoles.isEmpty()) {
            return new ArrayList<>();
        }
        
        return allUserRoles.stream()
                .filter(userRole -> userRole != null && userId.equals(userRole.getUserId()))
                .map(UserRoleDO::getRoleId)
                .filter(roleId -> roleId != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取角色菜单ID列表（从缓存中过滤）
     */
    private List<Long> getRoleMenuIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<RoleMenuDO> allRoleMenus = getAllRoleMenus();
        if (allRoleMenus == null || allRoleMenus.isEmpty()) {
            return new ArrayList<>();
        }
        
        return allRoleMenus.stream()
                .filter(roleMenu -> roleMenu != null && roleMenu.getRoleId() != null && roleIds.contains(roleMenu.getRoleId()))
                .map(RoleMenuDO::getMenuId)
                .filter(menuId -> menuId != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取角色权限ID列表（从缓存中过滤）
     */
    private List<Long> getRolePermissionIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<RolePermissionDO> allRolePermissions = getAllRolePermissions();
        if (allRolePermissions == null || allRolePermissions.isEmpty()) {
            return new ArrayList<>();
        }
        
        return allRolePermissions.stream()
                .filter(rolePermission -> rolePermission != null && rolePermission.getRoleId() != null && roleIds.contains(rolePermission.getRoleId()))
                .map(RolePermissionDO::getPermissionId)
                .filter(permissionId -> permissionId != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 构建菜单树
     */
    private List<Map<String, Object>> buildMenuTree(List<MenuDO> menus) {
        List<Map<String, Object>> menuMaps = menus.stream()
                .map(this::convertMenuToMap)
                .collect(Collectors.toList());

        // 构建树形结构
        Map<Long, Map<String, Object>> menuMap = new HashMap<>();
        List<Map<String, Object>> rootMenus = new ArrayList<>();

        for (Map<String, Object> menu : menuMaps) {
            Long id = (Long) menu.get("id");
            menuMap.put(id, menu);
            
            Long parentId = (Long) menu.get("parentId");
            if (parentId == null || parentId == 0) {
                rootMenus.add(menu);
            }
        }

        for (Map<String, Object> menu : menuMaps) {
            Long parentId = (Long) menu.get("parentId");
            if (parentId != null && parentId != 0) {
                Map<String, Object> parent = menuMap.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    if (children == null) {
                        children = new ArrayList<>();
                        parent.put("children", children);
                    }
                    children.add(menu);
                }
            }
        }

        return rootMenus;
    }

    /**
     * 转换菜单为Map
     */
    private Map<String, Object> convertMenuToMap(MenuDO menu) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", menu.getId());
        map.put("title", menu.getTitle());
        map.put("name", menu.getName());
        map.put("path", menu.getPath());
        map.put("component", menu.getComponent());
        map.put("icon", menu.getIcon());
        map.put("parentId", menu.getParentId());
        map.put("type", menu.getType());
        map.put("sort", menu.getSort());
        map.put("requiresAuth", menu.getRequiresAuth());
        map.put("status", menu.getStatus());
        return map;
    }

    /**
     * 转换权限为Map
     */
    private Map<String, Object> convertPermissionToMap(PermissionDO permission) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", permission.getId().toString());
        map.put("name", permission.getName());
        map.put("code", permission.getCode());
        map.put("type", permission.getType());
        map.put("parentId", permission.getParentId());
        map.put("path", permission.getPath());
        map.put("description", permission.getDescription());
        map.put("status", permission.getStatus());
        map.put("createTime", permission.getCreateTime() != null ? permission.getCreateTime().toString() : null);
        map.put("updateTime", permission.getUpdateTime() != null ? permission.getUpdateTime().toString() : null);
        return map;
    }

    /**
     * 转换角色为Map
     */
    private Map<String, Object> convertRoleToMap(RoleDO role) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("name", role.getName());
        map.put("code", role.getCode());
        map.put("description", role.getDescription());
        map.put("status", role.getStatus());
        map.put("sort", role.getSort());
        return map;
    }
}