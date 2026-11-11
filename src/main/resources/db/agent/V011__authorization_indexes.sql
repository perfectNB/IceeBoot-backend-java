-- 当前授权判定与用户列表批量查询使用的索引；只补索引，不改变授权数据。
ALTER TABLE t_user_role ADD INDEX idx_auth_user_roles (user_id, del_flag, role_id);
ALTER TABLE t_role_permission ADD INDEX idx_auth_role_permissions (role_id, del_flag, permission_id);
ALTER TABLE t_role_menu ADD INDEX idx_auth_role_menus (role_id, del_flag, menu_id);
ALTER TABLE t_permission ADD INDEX idx_auth_permission_codes (code, status, del_flag);
