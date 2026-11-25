-- 长期记忆整合至智能体配置；仅停用旧菜单入口，保留接口权限与关联以便回滚。
UPDATE t_menu SET hidden=1,status='inactive',update_time=NOW()
WHERE name='AgentMemory' AND del_flag=0;
