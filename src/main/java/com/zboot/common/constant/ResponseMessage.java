package com.zboot.common.constant;

/**
 * 响应消息常量类
 * 统一管理所有API返回的消息内容
 * 
 * @author CodeIcee
 * @since 2025-08-11
 */
public class ResponseMessage {

    // ==================== 通用消息 ====================
    /**
     * 操作成功
     */
    public static final String SUCCESS = "操作成功";
    
    /**
     * 操作失败
     */
    public static final String FAIL = "操作失败";
    
    // ==================== 认证相关 ====================
    /**
     * 密码错误
     */
    public static final String PASSWORD_ERROR = "密码错误";
    
    /**
     * 用户名或密码错误
     */
    public static final String LOGIN_ERROR = "用户名或密码错误";
    

    public static final String AUTH_FAIL = "认证失败";
    

    public static final String USER_NOT_EXIST = "用户不存在";


    public static final String USERNAME_OR_PASSWORD_EMPTY = "用户名或密码不能为空";


    public static final String ROLE_NAME_EXISTS = "角色名称已存在";
    

    public static final String ROLE_CODE_EXISTS = "角色编码已存在";
    

    public static final String MENU_NAME_EXISTS = "菜单名称已存在";
    

    public static final String PERMISSION_CODE_EXISTS = "权限编码已存在";
    
    public static final String SYSTEM_ERROR = "系统异常，请联系管理员";
    
    public static final String BAD_REQUEST = "请求参数错误";
    
    public static final String UNAUTHORIZED = "认证失败，无法访问系统资源";
    
    public static final String FORBIDDEN = "没有访问权限，请联系管理员授权";
    
    public static final String NOT_FOUND = "请求资源不存在";
    
    // ==================== 用户管理 ====================
    
    public static final String USER_CREATE_SUCCESS = "用户创建成功";
    
    public static final String USER_CREATE_FAIL = "用户创建失败";
    
    public static final String USER_UPDATE_SUCCESS = "用户更新成功";
    
    public static final String USER_UPDATE_FAIL = "用户更新失败";
    
    public static final String USER_DELETE_SUCCESS = "用户删除成功";
    
    public static final String USER_DELETE_FAIL = "用户删除失败";
    
    public static final String PASSWORD_RESET_SUCCESS = "密码重置成功";
    
    public static final String PASSWORD_RESET_FAIL = "密码重置失败";
    
    public static final String STATUS_UPDATE_SUCCESS = "状态修改成功";
    
    public static final String STATUS_UPDATE_FAIL = "状态修改失败";
    
    // ==================== AI模块 ====================
    
    public static final String CREATE_FAIL = "创建失败";
    
    public static final String UPDATE_FAIL = "更新失败";
    
    public static final String DELETE_FAIL = "删除失败";
    
    public static final String STATUS_UPDATE_FAIL_AI = "状态更新失败";
    
    // ==================== 角色管理 ====================
    
    public static final String ROLE_CREATE_SUCCESS = "角色创建成功";
    
    public static final String ROLE_CREATE_FAIL = "角色创建失败";
    
    public static final String ROLE_UPDATE_SUCCESS = "角色更新成功";
    
    public static final String ROLE_UPDATE_FAIL = "角色更新失败";
    
    public static final String ROLE_DELETE_SUCCESS = "角色删除成功";
    
    public static final String ROLE_DELETE_FAIL = "角色删除失败";
    
    // ==================== 菜单管理 ====================
    
    public static final String MENU_CREATE_SUCCESS = "菜单创建成功";
    
    public static final String MENU_CREATE_FAIL = "菜单创建失败";
    
    public static final String MENU_UPDATE_SUCCESS = "菜单更新成功";
    
    public static final String MENU_UPDATE_FAIL = "菜单更新失败";
    
    public static final String MENU_DELETE_SUCCESS = "菜单删除成功";
    
    public static final String MENU_DELETE_FAIL = "菜单删除失败";
    
    // ==================== 权限管理 ====================
    
    public static final String PERMISSION_CREATE_SUCCESS = "权限创建成功";
    
    public static final String PERMISSION_CREATE_FAIL = "权限创建失败";
    
    public static final String PERMISSION_UPDATE_SUCCESS = "权限更新成功";
    
    public static final String PERMISSION_UPDATE_FAIL = "权限更新失败";
    
    public static final String PERMISSION_DELETE_SUCCESS = "权限删除成功";
    
    public static final String PERMISSION_DELETE_FAIL = "权限删除失败";
    
    // ==================== 任务管理 ====================
    
    public static final String TASK_CREATE_SUCCESS = "任务创建成功";
    
    public static final String TASK_CREATE_FAIL = "任务创建失败";
    
    public static final String TASK_UPDATE_SUCCESS = "任务更新成功";
    
    public static final String TASK_UPDATE_FAIL = "任务更新失败";
    
    public static final String TASK_DELETE_SUCCESS = "任务删除成功";
    
    public static final String TASK_DELETE_FAIL = "任务删除失败";
    
    public static final String TASK_EXECUTE_SUCCESS = "任务执行成功";
    
    public static final String TASK_EXECUTE_FAIL = "任务执行失败";
    
    public static final String TASK_STOP_SUCCESS = "任务停止成功";
    
    public static final String TASK_STOP_FAIL = "任务停止失败";
    
    // ==================== 文件管理 ====================
    
    public static final String FILE_UPLOAD_SUCCESS = "文件上传成功";
    
    public static final String FILE_UPLOAD_FAIL = "文件上传失败";
    
    public static final String FILE_DOWNLOAD_SUCCESS = "文件下载成功";
    
    public static final String FILE_DOWNLOAD_FAIL = "文件下载失败";
    
    public static final String FILE_DELETE_SUCCESS = "文件删除成功";
    
    public static final String FILE_DELETE_FAIL = "文件删除失败";
    
    // ==================== 数据导入导出 ====================
    
    public static final String DATA_IMPORT_SUCCESS = "数据导入成功";
    
    public static final String DATA_IMPORT_FAIL = "数据导入失败";
    
    public static final String DATA_EXPORT_SUCCESS = "数据导出成功";
    
    public static final String DATA_EXPORT_FAIL = "数据导出失败";
    
    // ==================== 配置管理 ====================
    
    public static final String CONFIG_SAVE_SUCCESS = "配置保存成功";
    
    public static final String CONFIG_SAVE_FAIL = "配置保存失败";
    
    public static final String CONFIG_RESET_SUCCESS = "配置重置成功";
    
    public static final String CONFIG_RESET_FAIL = "配置重置失败";
    
    // ==================== 缓存管理 ====================
    
    public static final String CACHE_CLEAR_SUCCESS = "缓存清理成功";
    
    public static final String CACHE_CLEAR_FAIL = "缓存清理失败";
    
    public static final String CACHE_REFRESH_SUCCESS = "缓存刷新成功";
    
    public static final String CACHE_REFRESH_FAIL = "缓存刷新失败";
    
    // ==================== 私有构造函数 ====================
    
    private ResponseMessage() {
        // 工具类，禁止实例化
    }
}