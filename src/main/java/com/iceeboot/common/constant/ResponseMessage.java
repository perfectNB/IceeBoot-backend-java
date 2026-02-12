package com.iceeboot.common.constant;

/**
 * 响应消息常量类
 * 统一管理所有API返回的消息内容
 * 
 * @author CodeIcee
 * @data 2025-08-11
 */
public class ResponseMessage {

    public static class Common {
        public static final String SUCCESS = "操作成功";
        public static final String FAIL = "操作失败";
        public static final String ADD_SUCCESS = "新增成功";
        public static final String ADD_FAIL = "新增失败";
        public static final String UPDATE_SUCCESS = "更新成功";
        public static final String UPDATE_FAIL = "更新失败";
        public static final String DELETE_SUCCESS = "删除成功";
        public static final String DELETE_FAIL = "删除失败";
        public static final String STATUS_UPDATE_SUCCESS = "状态更新成功";
        public static final String STATUS_UPDATE_FAIL = "状态更新失败";
        public static final String SYSTEM_ERROR = "系统异常，请联系管理员";
        public static final String BAD_REQUEST = "请求参数错误";
        public static final String UNAUTHORIZED = "认证失败，无法访问系统资源";
        public static final String FORBIDDEN = "没有访问权限，请联系管理员授权";
        public static final String NOT_FOUND = "请求资源不存在";
        public static final String METHOD_NOT_ALLOWED = "请求方法不允许";
        public static final String RATE_LIMIT = "您请求的太快了，请等一下哦！";
    }

    public static class Auth {
        public static final String USER_NOT_EXIST = "用户不存在";
        public static final String USERNAME_OR_PASSWORD_EMPTY = "用户名或密码不能为空";
    }

    public static class User {
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
    }

    public static class Role {
        public static final String ROLE_NAME_EXISTS = "角色名称已存在";
        public static final String ROLE_CODE_EXISTS = "角色编码已存在";
        public static final String ROLE_CREATE_SUCCESS = "角色创建成功";
        public static final String ROLE_CREATE_FAIL = "角色创建失败";
        public static final String ROLE_UPDATE_SUCCESS = "角色更新成功";
        public static final String ROLE_UPDATE_FAIL = "角色更新失败";
        public static final String ROLE_DELETE_SUCCESS = "角色删除成功";
        public static final String ROLE_DELETE_FAIL = "角色删除失败";
    }

    public static class Menu {
        public static final String MENU_NAME_EXISTS = "菜单名称已存在";
        public static final String MENU_CREATE_SUCCESS = "菜单创建成功";
        public static final String MENU_CREATE_FAIL = "菜单创建失败";
        public static final String MENU_UPDATE_SUCCESS = "菜单更新成功";
        public static final String MENU_UPDATE_FAIL = "菜单更新失败";
        public static final String MENU_DELETE_SUCCESS = "菜单删除成功";
        public static final String MENU_DELETE_FAIL = "菜单删除失败";
    }

    public static class Permission {
        public static final String PERMISSION_CODE_EXISTS = "权限编码已存在";
        public static final String PERMISSION_CREATE_SUCCESS = "权限创建成功";
        public static final String PERMISSION_CREATE_FAIL = "权限创建失败";
        public static final String PERMISSION_UPDATE_SUCCESS = "权限更新成功";
        public static final String PERMISSION_UPDATE_FAIL = "权限更新失败";
        public static final String PERMISSION_DELETE_SUCCESS = "权限删除成功";
        public static final String PERMISSION_DELETE_FAIL = "权限删除失败";
    }

    public static class Ai {
        public static final String CREATE_SUCCESS = "创建成功";
        public static final String CREATE_FAIL = "创建失败";
    }

    public static class Task {
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
    }

    public static class File {
        public static final String FILE_UPLOAD_SUCCESS = "文件上传成功";
        public static final String FILE_UPLOAD_FAIL = "文件上传失败";
        public static final String FILE_DOWNLOAD_SUCCESS = "文件下载成功";
        public static final String FILE_DOWNLOAD_FAIL = "文件下载失败";
        public static final String FILE_DELETE_SUCCESS = "文件删除成功";
        public static final String FILE_DELETE_FAIL = "文件删除失败";
    }

    public static class Data {
        public static final String DATA_IMPORT_SUCCESS = "数据导入成功";
        public static final String DATA_IMPORT_FAIL = "数据导入失败";
        public static final String DATA_EXPORT_SUCCESS = "数据导出成功";
        public static final String DATA_EXPORT_FAIL = "数据导出失败";
    }

    public static class Config {
        public static final String CONFIG_SAVE_SUCCESS = "配置保存成功";
        public static final String CONFIG_SAVE_FAIL = "配置保存失败";
        public static final String CONFIG_RESET_SUCCESS = "配置重置成功";
        public static final String CONFIG_RESET_FAIL = "配置重置失败";
    }

    public static class Cache {
        public static final String CACHE_CLEAR_SUCCESS = "缓存清理成功";
        public static final String CACHE_CLEAR_FAIL = "缓存清理失败";
        public static final String CACHE_REFRESH_SUCCESS = "缓存刷新成功";
        public static final String CACHE_REFRESH_FAIL = "缓存刷新失败";
    }

    public static class Message {
        public static final String MESSAGE_CREATE_SUCCESS = "消息创建成功";
        public static final String MESSAGE_CREATE_FAIL = "消息创建失败";
        public static final String MESSAGE_UPDATE_SUCCESS = "消息更新成功";
        public static final String MESSAGE_UPDATE_FAIL = "消息更新失败";
        public static final String MESSAGE_DELETE_SUCCESS = "消息删除成功";
        public static final String MESSAGE_DELETE_FAIL = "消息删除失败";
    }

    public static class Factory {
        public static final String LLM_PROVIDER_EMPTY = "厂商名称不能为空";
        public static final String LLM_PROVIDER_NOT_SUPPORTED = "不支持的厂商: ";
        public static final String SMS_PROVIDER_EMPTY = "SMS厂商名称不能为空";
        public static final String SMS_PROVIDER_NOT_SUPPORTED = "不支持的SMS厂商: ";
        public static final String OSS_PROVIDER_EMPTY = "OSS厂商名称不能为空";
        public static final String OSS_PROVIDER_NOT_SUPPORTED = "不支持的OSS厂商: ";
        public static final String EMAIL_PROVIDER_EMPTY = "Email厂商名称不能为空";
        public static final String EMAIL_PROVIDER_NOT_SUPPORTED = "不支持的Email厂商: ";
        public static final String UNKNOWN_PROVIDER = "未知";
    }

    public static class AiChat {
        public static final String AI_CHAT_CONTENT_EMPTY = "对话内容不能为空";
    }


    public static class RolePermission {
        public static final String ROLE_PERMISSION_CREATE_SUCCESS = "新增角色权限关联成功";
        public static final String ROLE_PERMISSION_CREATE_FAIL = "新增角色权限关联失败";
        public static final String ROLE_PERMISSION_UPDATE_SUCCESS = "更新角色权限关联成功";
        public static final String ROLE_PERMISSION_UPDATE_FAIL = "更新角色权限关联失败";
        public static final String ROLE_PERMISSION_DELETE_SUCCESS = "删除角色权限关联成功";
        public static final String ROLE_PERMISSION_DELETE_FAIL = "删除角色权限关联失败";
        public static final String ROLE_PERMISSION_ASSIGN_SUCCESS = "分配权限成功";
        public static final String ROLE_PERMISSION_ASSIGN_FAIL = "分配权限失败";
        public static final String ROLE_PERMISSION_REMOVE_SUCCESS = "移除角色权限成功";
        public static final String ROLE_PERMISSION_REMOVE_FAIL = "移除角色权限失败";
        public static final String ROLE_PERMISSION_RELATION_CREATE_FAIL = "新增角色权限关联失败";
        public static final String ROLE_PERMISSION_RELATION_UPDATE_FAIL = "更新角色权限关联失败";
        public static final String ROLE_PERMISSION_RELATION_DELETE_FAIL = "删除角色权限关联失败";
        public static final String ROLE_PERMISSION_ASSIGN_SUCCESS_MSG = "角色权限分配成功";
        public static final String ROLE_PERMISSION_ASSIGN_FAIL_MSG = "角色权限分配失败";
    }

    public static class RoleMenu {
        public static final String ROLE_MENU_RELATION_CREATE_FAIL = "新增角色菜单关联失败";
        public static final String ROLE_MENU_RELATION_UPDATE_FAIL = "更新角色菜单关联失败";
        public static final String ROLE_MENU_RELATION_DELETE_FAIL = "删除角色菜单关联失败";
        public static final String ROLE_MENU_CREATE_SUCCESS = "新增角色菜单关联成功";
        public static final String ROLE_MENU_CREATE_FAIL = "新增角色菜单关联失败";
        public static final String ROLE_MENU_UPDATE_SUCCESS = "更新角色菜单关联成功";
        public static final String ROLE_MENU_UPDATE_FAIL = "更新角色菜单关联失败";
        public static final String ROLE_MENU_DELETE_SUCCESS = "删除角色菜单关联成功";
        public static final String ROLE_MENU_DELETE_FAIL = "删除角色菜单关联失败";
        public static final String ROLE_MENU_ASSIGN_SUCCESS = "分配菜单成功";
        public static final String ROLE_MENU_ASSIGN_FAIL = "分配菜单失败";
        public static final String ROLE_MENU_REMOVE_SUCCESS = "移除角色菜单成功";
        public static final String ROLE_MENU_REMOVE_FAIL = "移除角色菜单失败";
        public static final String ROLE_MENU_ASSIGN_SUCCESS_MSG = "角色菜单分配成功";
        public static final String ROLE_MENU_ASSIGN_FAIL_MSG = "角色菜单分配失败";
    }

    public static class UserRole {
        public static final String USER_ROLE_CREATE_SUCCESS = "新增用户角色关联成功";
        public static final String USER_ROLE_CREATE_FAIL = "新增用户角色关联失败";
        public static final String USER_ROLE_UPDATE_SUCCESS = "更新用户角色关联成功";
        public static final String USER_ROLE_UPDATE_FAIL = "更新用户角色关联失败";
        public static final String USER_ROLE_DELETE_SUCCESS = "删除用户角色关联成功";
        public static final String USER_ROLE_DELETE_FAIL = "删除用户角色关联失败";
        public static final String USER_ROLE_ASSIGN_SUCCESS = "分配角色成功";
        public static final String USER_ROLE_ASSIGN_FAIL = "分配角色失败";
        public static final String USER_ROLE_REMOVE_SUCCESS = "移除用户角色成功";
        public static final String USER_ROLE_REMOVE_FAIL = "移除用户角色失败";
        public static final String USER_ROLE_RELATION_CREATE_FAIL = "新增用户角色关联失败";
        public static final String USER_ROLE_RELATION_UPDATE_FAIL = "更新用户角色关联失败";
        public static final String USER_ROLE_RELATION_DELETE_FAIL = "删除用户角色关联失败";
    }


    public static class WorkflowMessage {
        public static final String PROCESS_DEFINITION_KEY_EMPTY = "流程定义Key不能为空";
        public static final String PROCESS_DEFINITION_ID_EMPTY = "流程定义ID不能为空";
        public static final String START_PROCESS_FAILED = "启动流程实例失败";
        public static final String TASK_NOT_EXIST_PREFIX = "任务不存在，任务ID: ";
        public static final String COMPLETE_TASK_FAILED = "完成任务失败";
        public static final String TASK_ID_EMPTY = "任务ID不能为空";
        public static final String TASK_CLAIM_USER_EMPTY = "认领用户不能为空";
        public static final String QUERY_USER_TASKS_FAILED = "查询用户任务失败";
        public static final String QUERY_CANDIDATE_TASKS_FAILED = "查询候选任务失败";
        public static final String PROCESS_FILE_EMPTY = "流程文件不能为空";
        public static final String DEPLOY_PROCESS_FAILED = "部署流程失败";
        public static final String UPDATE_PROCESS_DEFINITION_FAILED = "更新流程定义失败";
        public static final String DELETE_PROCESS_DEFINITION_FAILED = "删除流程定义失败";
        public static final String PROCESS_DEFINITION_NOT_EXIST = "流程定义不存在";
        public static final String PROCESS_DEFINITION_RESOURCE_NOT_EXIST = "流程定义资源不存在";
        public static final String GET_DEFINITION_XML_FAILED = "获取流程定义XML失败";
        public static final String PROCESS_DEFINITION_DIAGRAM_NOT_EXIST = "流程定义图片不存在";
        public static final String GET_DEFINITION_DIAGRAM_FAILED = "获取流程定义图失败";
        public static final String PROCESS_INSTANCE_NOT_EXIST = "流程实例不存在";
        public static final String QUERY_PROCESS_INSTANCE_FAILED = "查询流程实例失败";
        public static final String QUERY_PROCESS_INSTANCES_FAILED = "查询流程实例列表失败";
        public static final String DELETE_PROCESS_INSTANCE_FAILED = "删除流程实例失败";
        public static final String PROCESS_INSTANCE_ID_EMPTY = "流程实例ID不能为空";
        public static final String PROCESS_ENDED_CHECK_FAILED = "判断流程是否结束失败";
        public static final String TARGET_ACTIVITY_ID_EMPTY = "目标节点ID不能为空";
        public static final String JUMP_TASK_FAILED = "跳转任务失败";
        public static final String JUMP_TASK_NEED_IMPLEMENT = "跳转功能需要根据具体业务需求实现，任务ID: {}, 目标节点: {}";
        public static final String TASK_CLAIM_FAILED = "认领任务失败";
        public static final String TASK_UNCLAIM_FAILED = "取消认领任务失败";
        public static final String END_PROCESS_FAILED = "结束流程失败";
    }

    private ResponseMessage() {
        // 工具类，禁止实例化
    }
}
