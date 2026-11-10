/**
 * 认证缓存服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.exception.AuthServiceUnavailableException;
import com.iceeboot.framework.entity.system.*;
import com.iceeboot.framework.mapper.system.*;
import com.iceeboot.framework.service.system.IAuthCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

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
@RequiredArgsConstructor
public class AuthCacheServiceImpl implements IAuthCacheService {

    // 直接使用CacheConstants中的常量

    private final RedisTemplate<String, Object> redisTemplate;

    private final SysMenuMapper menuMapper;

    private final SysPermissionMapper permissionMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final SysRoleMenuMapper roleMenuMapper;

    private final SysRolePermissionMapper rolePermissionMapper;


    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserMenus(Long userId) {

//        String key = CacheConstants.Auth.USER_MENUS_KEY + userId;
//        List<Map<String, Object>> menus = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
//
//        if (menus == null) {
//            menus = loadUserMenusFromDB(userId);
//            redisTemplate.opsForValue().set(key, menus, CacheConstants.Auth.CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
//        }

        //    return menus;

        // 通过用户ID获取角色列表
        String userRoleKey = CacheConstants.Auth.USER_ROLE + userId;
        List<Long> roleIds = (List<Long>) redisTemplate.opsForValue().get(userRoleKey);

        if (roleIds == null || roleIds.isEmpty()) {
            roleIds = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRoleDO>()
                            .eq(UserRoleDO::getUserId, userId)
                            .eq(UserRoleDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
            ).stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());
            redisTemplate.opsForValue().set(userRoleKey, roleIds);
        }

        // 遍历角色列表，获取每个角色的菜单
        Set<MenuDO> menuSet = new HashSet<>();
        for (Long roleId : roleIds) {
            String roleMenuKey = CacheConstants.Auth.ROLE_MENUS_KEY + roleId;
            List<MenuDO> roleMenus;
            roleMenus = (List<MenuDO>) redisTemplate.opsForValue().get(roleMenuKey);

            if (roleMenus != null) {
                menuSet.addAll(roleMenus);
            } else {
                List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<RoleMenuDO>()
                                .eq(RoleMenuDO::getRoleId, roleId)
                                .eq(RoleMenuDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                ).stream().map(RoleMenuDO::getMenuId).collect(Collectors.toList());
                if (menuIds.size() != 0) {
                    roleMenus = menuMapper.selectList(
                            new LambdaQueryWrapper<MenuDO>()
                                    .in(MenuDO::getId, menuIds)
                                    .eq(MenuDO::getStatus, SystemConstants.Status.ACTIVE)
                                    .eq(MenuDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                                    .orderByAsc(MenuDO::getSort));
                    menuSet.addAll(roleMenus);
                    redisTemplate.opsForValue().set(
                            roleMenuKey,
                            roleMenus
                    );
                }
            }
        }
        // 将Set转换为List并按sort排序
        List<MenuDO> menuList = new ArrayList<>(menuSet);
        menuList.sort((m1, m2) -> {
            Integer sort1 = m1.getSort();
            Integer sort2 = m2.getSort();
            if (sort1 == null) sort1 = 0;
            if (sort2 == null) sort2 = 0;
            return sort1.compareTo(sort2);
        });
        // 构建树形结构
        return buildMenuTree(menuList);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserPermissions(Long userId) {
        // 1. 通过用户ID获取角色列表
        String userRoleKey = CacheConstants.Auth.USER_ROLE + userId;
        List<Long> roleIds = (List<Long>) redisTemplate.opsForValue().get(userRoleKey);

        if (roleIds == null || roleIds.isEmpty()) {
            roleIds = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRoleDO>()
                            .eq(UserRoleDO::getUserId, userId)
                            .eq(UserRoleDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
            ).stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());
            redisTemplate.opsForValue().set(userRoleKey, roleIds);
        }

        // 2. 遍历角色列表，获取每个角色的权限
        Set<Map<String, Object>> permissionSet = new HashSet<>();
        for (Long roleId : roleIds) {
            String rolePermissionKey = CacheConstants.Auth.ROLE_PERMISSIONS_KEY + roleId;
            List<PermissionDO> rolePermissions;
            List<Map<String, Object>> permissionMaps;
            rolePermissions = (List<PermissionDO>) redisTemplate.opsForValue().get(rolePermissionKey);
            if (rolePermissions != null) {
                // 将PermissionDO转换为Map<String, Object>
                permissionMaps = rolePermissions.stream()
                        .map(this::convertPermissionToMap)
                        .collect(Collectors.toList());
                permissionSet.addAll(permissionMaps);
            } else {
                List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermissionDO>()
                                .eq(RolePermissionDO::getRoleId, roleId)
                                .eq(RolePermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                ).stream().map(RolePermissionDO::getPermissionId).collect(Collectors.toList());
                if (permissionIds.size() != 0) {
                    rolePermissions = permissionMapper.selectList(
                            new LambdaQueryWrapper<PermissionDO>()
                                    .in(PermissionDO::getId, permissionIds)
                                    .eq(PermissionDO::getStatus, SystemConstants.Status.ACTIVE)
                                    .eq(PermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                                    .orderByAsc(PermissionDO::getSort));
                    permissionMaps = rolePermissions.stream()
                            .map(this::convertPermissionToMap)
                            .collect(Collectors.toList());
                    permissionSet.addAll(permissionMaps);
                    redisTemplate.opsForValue().set(rolePermissionKey, rolePermissions);
                }
            }
        }

        // 3. 返回去重后的权限列表
        return new ArrayList<>(permissionSet);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserRoles(Long userId) {
        String key = CacheConstants.Auth.USER_ROLES_KEY + userId;
        List<Map<String, Object>> roles = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);

        if (roles == null) {
            roles = loadUserRolesFromDB(userId);
            redisTemplate.opsForValue().set(key, roles, CacheConstants.Auth.CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }

        return roles;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllMenus() {
        List<Map<String, Object>> menus = (List<Map<String, Object>>) redisTemplate.opsForValue().get(CacheConstants.Auth.ALL_MENUS_KEY);

        if (menus == null) {
            menus = loadAllMenusFromDB();
            redisTemplate.opsForValue().set(CacheConstants.Auth.ALL_MENUS_KEY, menus, CacheConstants.Auth.CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }

        return menus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllPermissions() {
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) redisTemplate.opsForValue().get(CacheConstants.Auth.ALL_PERMISSIONS_KEY);

        if (permissions == null) {
            permissions = loadAllPermissionsFromDB();
            redisTemplate.opsForValue().set(CacheConstants.Auth.ALL_PERMISSIONS_KEY, permissions, CacheConstants.Auth.CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
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
        List<RoleDO> roles = (List<RoleDO>) redisTemplate.opsForValue().get(CacheConstants.Auth.ALL_ROLES_KEY);

        if (roles == null) {
            roles = loadAllRolesFromDB();
            redisTemplate.opsForValue().set(CacheConstants.Auth.ALL_ROLES_KEY, roles, CacheConstants.Auth.CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
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
        List<UserRoleDO> userRoles = (List<UserRoleDO>) redisTemplate.opsForValue().get(CacheConstants.Auth.ALL_USER_ROLES_KEY);

        if (userRoles == null) {
            userRoles = loadAllUserRolesFromDB();
            redisTemplate.opsForValue().set(CacheConstants.Auth.ALL_USER_ROLES_KEY, userRoles);
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
        List<RoleMenuDO> roleMenus = (List<RoleMenuDO>) redisTemplate.opsForValue().get(CacheConstants.Auth.ALL_ROLE_MENUS_KEY);

        if (roleMenus == null) {
            roleMenus = loadAllRoleMenusFromDB();
            redisTemplate.opsForValue().set(CacheConstants.Auth.ALL_ROLE_MENUS_KEY, roleMenus);
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
        List<RolePermissionDO> rolePermissions = (List<RolePermissionDO>) redisTemplate.opsForValue().get(CacheConstants.Auth.ALL_ROLE_PERMISSIONS_KEY);

        if (rolePermissions == null) {
            rolePermissions = loadAllRolePermissionsFromDB();
            redisTemplate.opsForValue().set(CacheConstants.Auth.ALL_ROLE_PERMISSIONS_KEY, rolePermissions);
        }

        return rolePermissions != null ? rolePermissions : new ArrayList<>();
    }

    @Override
    public void clearUserCache(Long userId) {
        redisTemplate.delete(CacheConstants.Auth.USER_MENUS_KEY + userId);
        redisTemplate.delete(CacheConstants.Auth.USER_PERMISSIONS_KEY + userId);
        redisTemplate.delete(CacheConstants.Auth.USER_ROLES_KEY + userId);
    }

    @Override
    public void clearAllCache() {
        // 清除全量数据缓存
        redisTemplate.delete(CacheConstants.Auth.ALL_MENUS_KEY);
        redisTemplate.delete(CacheConstants.Auth.ALL_PERMISSIONS_KEY);
        redisTemplate.delete(CacheConstants.Auth.ALL_ROLES_KEY);
        redisTemplate.delete(CacheConstants.Auth.ALL_USER_ROLES_KEY);
        redisTemplate.delete(CacheConstants.Auth.ALL_ROLE_MENUS_KEY);
        redisTemplate.delete(CacheConstants.Auth.ALL_ROLE_PERMISSIONS_KEY);

        // 清除所有用户缓存
        Set<String> userMenuKeys = redisTemplate.keys(CacheConstants.Auth.USER_MENUS_KEY + "*");
        Set<String> userPermissionKeys = redisTemplate.keys(CacheConstants.Auth.USER_PERMISSIONS_KEY + "*");
        Set<String> userRoleKeys = redisTemplate.keys(CacheConstants.Auth.USER_ROLES_KEY + "*");

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

        // 直接从已构建的菜单树中过滤用户有权限的菜单，保持树形结构
        return filterMenusByPermission(allMenus, menuIds);
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
                .eq(MenuDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                .orderByAsc(MenuDO::getSort));
        return buildMenuTree(menus);
    }

    /**
     * 从数据库加载所有权限
     */
    private List<Map<String, Object>> loadAllPermissionsFromDB() {
        List<PermissionDO> permissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionDO>()
                .eq(PermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
        return permissions.stream().map(this::convertPermissionToMap).collect(Collectors.toList());
    }

    /**
     * 从数据库加载所有角色
     */
    private List<RoleDO> loadAllRolesFromDB() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
    }

    /**
     * 从数据库加载所有用户角色关系
     */
    private List<UserRoleDO> loadAllUserRolesFromDB() {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
    }

    /**
     * 从数据库加载所有角色菜单关系
     */
    private List<RoleMenuDO> loadAllRoleMenusFromDB() {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenuDO>()
                .eq(RoleMenuDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
    }

    /**
     * 从数据库加载所有角色权限关系
     */
    private List<RolePermissionDO> loadAllRolePermissionsFromDB() {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermissionDO>()
                .eq(RolePermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED));
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
     * 从Map列表构建菜单树形结构
     */
    private List<Map<String, Object>> buildMenuTreeFromMaps(List<Map<String, Object>> menuMaps) {
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

        // 对每个层级的菜单按sort排序
        sortMenuChildren(rootMenus);

        return rootMenus;
    }

    /**
     * 递归对菜单子项按sort排序
     */
    private void sortMenuChildren(List<Map<String, Object>> menus) {
        if (menus == null || menus.isEmpty()) {
            return;
        }

        // 对当前层级排序
        menus.sort((m1, m2) -> {
            Integer sort1 = (Integer) m1.get("sort");
            Integer sort2 = (Integer) m2.get("sort");
            if (sort1 == null) sort1 = 0;
            if (sort2 == null) sort2 = 0;
            return sort1.compareTo(sort2);
        });

        // 递归对子菜单排序
        for (Map<String, Object> menu : menus) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) menu.get("children");
            if (children != null && !children.isEmpty()) {
                sortMenuChildren(children);
            }
        }
    }

    /**
     * 从菜单树中过滤用户有权限的菜单，保持树形结构
     */
    private List<Map<String, Object>> filterMenusByPermission(List<Map<String, Object>> menus, List<Long> menuIds) {
        if (menus == null || menus.isEmpty() || menuIds == null || menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> filteredMenus = new ArrayList<>();

        for (Map<String, Object> menu : menus) {
            Object idObj = menu.get("id");
            if (idObj != null) {
                Long menuId = Long.valueOf(idObj.toString());

                // 检查当前菜单是否有权限
                if (menuIds.contains(menuId)) {
                    Map<String, Object> filteredMenu = new HashMap<>(menu);

                    // 递归处理子菜单
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) menu.get("children");
                    if (children != null && !children.isEmpty()) {
                        List<Map<String, Object>> filteredChildren = filterMenusByPermission(children, menuIds);
                        filteredMenu.put("children", filteredChildren);
                    }

                    filteredMenus.add(filteredMenu);
                } else {
                    // 即使当前菜单没有权限，也要检查子菜单是否有权限
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) menu.get("children");
                    if (children != null && !children.isEmpty()) {
                        List<Map<String, Object>> filteredChildren = filterMenusByPermission(children, menuIds);
                        if (!filteredChildren.isEmpty()) {
                            // 如果子菜单有权限，则包含父菜单
                            Map<String, Object> filteredMenu = new HashMap<>(menu);
                            filteredMenu.put("children", filteredChildren);
                            filteredMenus.add(filteredMenu);
                        }
                    }
                }
            }
        }

        return filteredMenus;
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
        map.put("link", menu.getLink());
        map.put("requiresAuth", menu.getRequiresAuth());

        // 处理permissions字段，将逗号分隔的字符串转换为字符串数组
        String permissionsStr = menu.getPermissions();
        if (permissionsStr != null && !permissionsStr.trim().isEmpty()) {
            String[] permissionsArray = permissionsStr.split(",");
            List<String> permissionsList = Arrays.stream(permissionsArray)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            map.put("permissions", permissionsList);
        } else {
            map.put("permissions", new ArrayList<>());
        }

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

    private static final AntPathMatcher URL_MATCHER = new AntPathMatcher();
    private static final Set<String> HTTP_METHODS =
            Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "*");

    @Override
    public List<String> getUrlPermissions(String url) {
        return matchUrlPermissions(url, null);
    }

    @Override
    public List<String> getUrlPermissions(String url, String method) {
        if (!StringUtils.hasText(method)) return List.of();
        return matchUrlPermissions(url, method.toUpperCase(Locale.ROOT));
    }

    private List<String> matchUrlPermissions(String url, String method) {
        if (!StringUtils.hasText(url)) return List.of();
        Map<String, List<String>> mappings = getAllUrlPermissions();
        Comparator<String> paths = URL_MATCHER.getPatternComparator(url);
        List<Map.Entry<String, List<String>>> matches = mappings.entrySet().stream()
                .filter(entry -> method == null || routeMethod(entry.getKey()).equals(method)
                        || routeMethod(entry.getKey()).equals("*"))
                .filter(entry -> URL_MATCHER.match(routePath(entry.getKey()), url))
                .sorted((left, right) -> {
                    int comparison = paths.compare(routePath(left.getKey()), routePath(right.getKey()));
                    if (comparison != 0) return comparison;
                    if (method != null) {
                        comparison = Boolean.compare(routeMethod(left.getKey()).equals("*"), routeMethod(right.getKey()).equals("*"));
                        if (comparison != 0) return comparison;
                    }
                    return left.getKey().compareTo(right.getKey());
                }).toList();
        if (matches.isEmpty()) return List.of();
        if (method != null) {
            List<String> permissions = matches.getFirst().getValue();
            return permissions == null ? List.of() : List.copyOf(permissions);
        }
        // 无方法参数的旧调用仅用于查询，不参与拦截器授权。
        String selectedPath = routePath(matches.getFirst().getKey());
        return matches.stream().filter(entry -> routePath(entry.getKey()).equals(selectedPath))
                .map(Map.Entry::getValue).filter(Objects::nonNull).flatMap(List::stream)
                .filter(StringUtils::hasText).distinct().toList();
    }

    private static String routeMethod(String key) {
        return key.substring(0, key.indexOf(' '));
    }

    private static String routePath(String key) {
        return key.substring(key.indexOf(' ') + 1);
    }

    private static boolean isMethodAwareKey(String key) {
        if (key == null) return false;
        int separator = key.indexOf(' ');
        return separator > 0 && HTTP_METHODS.contains(key.substring(0, separator))
                && key.substring(separator + 1).startsWith("/");
    }

    @Override
    public void setUrlPermissions(String url, List<String> permissions) {
        if (!StringUtils.hasText(url)) return;
        Map<String, List<String>> mappings = new HashMap<>(getAllUrlPermissions());
        // 保留旧设置接口的“所有 HTTP 方法”语义，方法专属配置由权限表维护。
        String key = "* " + url;
        mappings.keySet().removeIf(route -> routePath(route).equals(url));
        if (permissions == null || permissions.isEmpty()) mappings.remove(key);
        else mappings.put(key, List.copyOf(permissions));
        redisTemplate.opsForValue().set(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY, mappings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getAllUrlPermissions() {
        Map<String, List<String>> mappings = (Map<String, List<String>>) redisTemplate.opsForValue()
                .get(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY);
        // 旧缓存只按 URL 存储，不能直接参与新的方法级授权。
        if (mappings == null || mappings.keySet().stream().anyMatch(key -> !isMethodAwareKey(key))) {
            mappings = loadUrlPermissionsFromDB();
            redisTemplate.opsForValue().set(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY, mappings);
        }
        return mappings;
    }

    @Override
    public void clearUrlPermissionCache() {
        redisTemplate.delete(CacheConstants.Auth.URL_PERMISSION_MAPPING_KEY);
    }

    private Map<String, List<String>> loadUrlPermissionsFromDB() {
        Map<String, List<String>> mappings = new HashMap<>();
        try {
            List<PermissionDO> permissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionDO>()
                    .eq(PermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                    .eq(PermissionDO::getStatus, SystemConstants.Status.ACTIVE)
                    .eq(PermissionDO::getType, "api")
                    .isNotNull(PermissionDO::getPath).ne(PermissionDO::getPath, ""));
            for (PermissionDO permission : permissions) {
                String path = permission.getPath();
                String code = permission.getCode();
                if (!StringUtils.hasText(path) || !StringUtils.hasText(code)) continue;
                String methods = StringUtils.hasText(permission.getMethod()) ? permission.getMethod().trim().toUpperCase(Locale.ROOT) : "*";
                for (String method : methods.split("[,\\s]+")) {
                    if (!HTTP_METHODS.contains(method)) continue;
                    List<String> codes = mappings.computeIfAbsent(method + " " + path, key -> new ArrayList<>());
                    if (!codes.contains(code)) codes.add(code);
                }
            }
        } catch (Exception e) {
            throw new AuthServiceUnavailableException(e);
        }
        return mappings;
    }
}
