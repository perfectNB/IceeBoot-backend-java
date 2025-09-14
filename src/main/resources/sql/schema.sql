-- iceeboot超级脚手架数据库表结构
-- @Description: 数据库表结构初始化脚本
-- @Date: 2025-01-11
-- @Author: CodeIcee

-- 活动表
CREATE TABLE IF NOT EXISTS `activity` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_name` varchar(100) NOT NULL COMMENT '活动名称',
    `description` text COMMENT '活动描述',
    `activity_type` varchar(50) COMMENT '活动类型',
    `status` int DEFAULT '0' COMMENT '活动状态（0-未开始，1-进行中，2-已结束，3-已取消）',
    `start_time` datetime COMMENT '开始时间',
    `end_time` datetime COMMENT '结束时间',
    `location` varchar(200) COMMENT '活动地点',
    `participant_limit` int COMMENT '参与人数限制',
    `current_participants` int DEFAULT '0' COMMENT '当前参与人数',
    `organizer` varchar(100) COMMENT '活动负责人',
    `contact_info` varchar(200) COMMENT '联系方式',
    `fee` decimal(10,2) DEFAULT '0.00' COMMENT '活动费用',
    `registration_deadline` datetime COMMENT '报名截止时间',
    `tags` varchar(500) COMMENT '活动标签',
    `image_url` varchar(500) COMMENT '活动图片URL',
    `require_approval` int DEFAULT '0' COMMENT '是否需要审核（0-不需要，1-需要）',
    `remarks` text COMMENT '备注信息',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_activity_name` (`activity_name`),
    KEY `idx_activity_type` (`activity_type`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动信息表';

-- 任务作业表
CREATE TABLE IF NOT EXISTS `job` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_name` varchar(100) NOT NULL COMMENT '作业名称',
    `description` text COMMENT '作业描述',
    `job_type` varchar(50) COMMENT '作业类型',
    `status` int DEFAULT '0' COMMENT '作业状态（0-待执行，1-执行中，2-已完成，3-失败，4-已取消）',
    `priority` int DEFAULT '2' COMMENT '优先级（1-低，2-中，3-高，4-紧急）',
    `scheduled_start_time` datetime COMMENT '计划开始时间',
    `scheduled_end_time` datetime COMMENT '计划结束时间',
    `actual_start_time` datetime COMMENT '实际开始时间',
    `actual_end_time` datetime COMMENT '实际结束时间',
    `executor_class` varchar(200) COMMENT '执行器类名',
    `parameters` text COMMENT '执行参数（JSON格式）',
    `cron_expression` varchar(100) COMMENT 'Cron表达式（定时任务）',
    `retry_count` int DEFAULT '0' COMMENT '重试次数',
    `max_retry_count` int DEFAULT '3' COMMENT '最大重试次数',
    `result` text COMMENT '执行结果',
    `error_message` text COMMENT '错误信息',
    `execution_time` bigint COMMENT '执行耗时（毫秒）',
    `assignee` varchar(100) COMMENT '负责人',
    `job_group` varchar(100) COMMENT '作业组',
    `enabled` int DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
    `next_execution_time` datetime COMMENT '下次执行时间',
    `remarks` text COMMENT '备注信息',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_job_name` (`job_name`),
    KEY `idx_job_type` (`job_type`),
    KEY `idx_status` (`status`),
    KEY `idx_priority` (`priority`),
    KEY `idx_scheduled_start_time` (`scheduled_start_time`),
    KEY `idx_job_group` (`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务作业表';

-- 系统用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(30) NOT NULL COMMENT '用户账号',
    `nickname` varchar(30) NOT NULL COMMENT '用户昵称',
    `email` varchar(50) COMMENT '用户邮箱',
    `phone` varchar(11) COMMENT '手机号码',
    `sex` char(1) COMMENT '用户性别（0男 1女 2未知）',
    `avatar` varchar(100) COMMENT '头像地址',
    `password` varchar(100) COMMENT '密码',
    `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
    `login_ip` varchar(128) COMMENT '最后登录IP',
    `login_date` datetime COMMENT '最后登录时间',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `remark` varchar(500) COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 系统角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` varchar(30) NOT NULL COMMENT '角色名称',
    `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
    `role_sort` int NOT NULL COMMENT '显示顺序',
    `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
    `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
    `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
    `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `remark` varchar(500) COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`),
    KEY `idx_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

-- 用户和角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

-- 系统菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
    `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
    `order_num` int DEFAULT '0' COMMENT '显示顺序',
    `path` varchar(200) COMMENT '路由地址',
    `component` varchar(255) COMMENT '组件路径',
    `query` varchar(255) COMMENT '路由参数',
    `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
    `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
    `menu_type` char(1) COMMENT '菜单类型（M目录 C菜单 F按钮）',
    `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
    `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
    `perms` varchar(100) COMMENT '权限标识',
    `icon` varchar(100) COMMENT '菜单图标',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `remark` varchar(500) COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_menu_name` (`menu_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- 角色和菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `menu_id` bigint NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';

-- AI代理表
CREATE TABLE IF NOT EXISTS `ai_agent` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `agent_name` varchar(100) NOT NULL COMMENT '代理名称',
    `description` text COMMENT '代理描述',
    `agent_type` varchar(50) COMMENT '代理类型',
    `model_name` varchar(100) COMMENT '模型名称',
    `system_prompt` text COMMENT '系统提示词',
    `temperature` decimal(3,2) DEFAULT '0.70' COMMENT '温度参数',
    `max_tokens` int DEFAULT '2048' COMMENT '最大令牌数',
    `status` int DEFAULT '1' COMMENT '状态（0-禁用，1-启用）',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_agent_name` (`agent_name`),
    KEY `idx_agent_type` (`agent_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI代理表';

-- AI聊天会话表
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_name` varchar(100) COMMENT '会话名称',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `agent_id` bigint NOT NULL COMMENT '代理ID',
    `status` int DEFAULT '1' COMMENT '状态（0-已结束，1-进行中）',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天会话表';

-- AI聊天消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` bigint NOT NULL COMMENT '会话ID',
    `message_type` varchar(20) NOT NULL COMMENT '消息类型（user/assistant/system）',
    `content` text NOT NULL COMMENT '消息内容',
    `tokens` int COMMENT '令牌数量',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_message_type` (`message_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

-- AI大语言模型表
CREATE TABLE IF NOT EXISTS `ai_llm` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `model_name` varchar(100) NOT NULL COMMENT '模型名称',
    `provider` varchar(50) NOT NULL COMMENT '提供商',
    `api_url` varchar(200) COMMENT 'API地址',
    `model_type` varchar(50) COMMENT '模型类型',
    `max_tokens` int DEFAULT '4096' COMMENT '最大令牌数',
    `status` int DEFAULT '1' COMMENT '状态（0-禁用，1-启用）',
    `description` text COMMENT '模型描述',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_name` (`model_name`),
    KEY `idx_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI大语言模型表';

-- AI模型密钥表
CREATE TABLE IF NOT EXISTS `ai_llm_key` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `llm_id` bigint NOT NULL COMMENT '模型ID',
    `key_name` varchar(100) NOT NULL COMMENT '密钥名称',
    `api_key` varchar(500) NOT NULL COMMENT 'API密钥',
    `status` int DEFAULT '1' COMMENT '状态（0-禁用，1-启用）',
    `usage_count` bigint DEFAULT '0' COMMENT '使用次数',
    `last_used_time` datetime COMMENT '最后使用时间',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_llm_id` (`llm_id`),
    KEY `idx_key_name` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型密钥表';

-- 任务表
CREATE TABLE IF NOT EXISTS `task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_name` varchar(100) NOT NULL COMMENT '任务名称',
    `description` text COMMENT '任务描述',
    `task_type` varchar(50) COMMENT '任务类型',
    `status` int DEFAULT '0' COMMENT '任务状态（0-待处理，1-处理中，2-已完成，3-已取消）',
    `priority` int DEFAULT '2' COMMENT '优先级（1-低，2-中，3-高，4-紧急）',
    `assignee` varchar(100) COMMENT '负责人',
    `due_date` datetime COMMENT '截止时间',
    `progress` int DEFAULT '0' COMMENT '进度百分比',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_task_name` (`task_name`),
    KEY `idx_status` (`status`),
    KEY `idx_assignee` (`assignee`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- 日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `log_type` varchar(50) COMMENT '日志类型',
    `log_level` varchar(20) COMMENT '日志级别',
    `module` varchar(100) COMMENT '模块名称',
    `operation` varchar(200) COMMENT '操作内容',
    `method` varchar(200) COMMENT '请求方法',
    `request_url` varchar(500) COMMENT '请求URL',
    `request_ip` varchar(128) COMMENT '请求IP',
    `user_agent` varchar(500) COMMENT '用户代理',
    `request_params` text COMMENT '请求参数',
    `response_data` text COMMENT '响应数据',
    `execution_time` bigint COMMENT '执行时间（毫秒）',
    `error_message` text COMMENT '错误信息',
    `create_by` varchar(64) COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` varchar(64) COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`),
    KEY `idx_log_type` (`log_type`),
    KEY `idx_module` (`module`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

-- 插入默认管理员用户
INSERT IGNORE INTO `sys_user` (`id`, `username`, `nickname`, `email`, `phone`, `sex`, `password`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (1, 'admin', '管理员', 'admin@iceeboot.com', '13888888888', '0', '$2a$10$7JB720yubVSOfvVMe6/b.eqJWjkXMdydqvcgs1RE9pmOig.jy.jG.', '0', 'system', NOW(), '管理员账户');

-- 插入默认角色
INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (1, '超级管理员', 'admin', 1, '0', 'system', NOW(), '超级管理员角色');

-- 插入用户角色关联
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 插入默认菜单
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1, '系统管理', 0, 1, 'system', NULL, 'M', '0', '0', '', 'system', 'system', NOW(), '系统管理目录'),
(2, '用户管理', 1, 1, 'user', 'system/user/index', 'C', '0', '0', 'system:user:list', 'user', 'system', NOW(), '用户管理菜单'),
(3, '角色管理', 1, 2, 'role', 'system/role/index', 'C', '0', '0', 'system:role:list', 'peoples', 'system', NOW(), '角色管理菜单'),
(4, '菜单管理', 1, 3, 'menu', 'system/menu/index', 'C', '0', '0', 'system:menu:list', 'tree-table', 'system', NOW(), '菜单管理菜单'),
(5, '活动管理', 0, 2, 'activity', 'activity/index', 'C', '0', '0', 'activity:list', 'calendar', 'system', NOW(), '活动管理菜单'),
(6, '任务管理', 0, 3, 'job', 'job/index', 'C', '0', '0', 'job:list', 'job', 'system', NOW(), '任务管理菜单');

-- 插入角色菜单关联
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES 
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);