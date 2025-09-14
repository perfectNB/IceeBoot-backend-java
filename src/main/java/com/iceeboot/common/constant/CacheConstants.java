/**
 * 缓存常量类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.common.constant;

/**
 * 缓存常量类
 * 统一管理所有缓存键
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
public class CacheConstants {

    /**
     * 认证相关缓存键
     */
    public static class Auth {
        
        /** 用户菜单缓存键前缀 */
        public static final String USER_MENUS_KEY = "auth:user:menus:";
        
        /** 用户权限缓存键前缀 */
        public static final String USER_PERMISSIONS_KEY = "auth:user:permissions:";
        
        /** 用户角色缓存键前缀 */
        public static final String USER_ROLES_KEY = "auth:user:roles:";
        
        /** 所有菜单缓存键 */
        public static final String ALL_MENUS_KEY = "auth:all:menus";
        
        /** 所有权限缓存键 */
        public static final String ALL_PERMISSIONS_KEY = "auth:all:permissions";
        
        /** 所有角色缓存键 */
        public static final String ALL_ROLES_KEY = "auth:all:roles";
        
        /** 所有用户角色关系缓存键 */
        public static final String ALL_USER_ROLES_KEY = "auth:all:user_roles";
        
        /** 所有角色菜单关系缓存键 */
        public static final String ALL_ROLE_MENUS_KEY = "auth:all:role_menus";
        
        /** 所有角色权限关系缓存键 */
        public static final String ALL_ROLE_PERMISSIONS_KEY = "auth:all:role_permissions";
        
        /** URL权限映射缓存键 */
        public static final String URL_PERMISSION_MAPPING_KEY = "auth:url:permission:mapping";
        
        /** 角色菜单缓存键 */
        public static final String ROLE_MENUS_KEY = "role:menus:";

        /** 角色权限缓存键 */
        public static final String ROLE_PERMISSIONS_KEY = "role:permissions:";

         /** 用户角色缓存键 */
        public static final String USER_ROLE = "user:role:";
        
        /** 缓存过期时间（分钟） */
        public static final long CACHE_EXPIRE_TIME = 30;

        /** 会话缓存键前缀 */
        public static final String SESSION_CACHE_KEY = "chat:session:";

        /** 密钥缓存键前缀 */
        public static final String KEY_CACHE_KEY = "chat:key:";
    }
    
    /**
     * 业务相关缓存键
     */
    public static class Business {
        
        /** 系统配置缓存键前缀 */
        public static final String SYSTEM_CONFIG_KEY = "business:config:";
        
        /** 字典数据缓存键前缀 */
        public static final String DICT_DATA_KEY = "business:dict:";
    }
}