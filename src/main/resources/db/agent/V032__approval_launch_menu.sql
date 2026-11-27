UPDATE t_menu SET title='发起审批' WHERE name='WorkflowDiagram' AND title='流程图展示' AND del_flag=0;

-- Existing applicants with access to their applications can use the dedicated launch page.
INSERT INTO t_role_menu(role_id,menu_id,create_time,update_time,del_flag)
SELECT DISTINCT rm.role_id,target.id,NOW(),NOW(),0
FROM t_role_menu rm
JOIN t_menu source_menu ON source_menu.id=rm.menu_id AND source_menu.name='WorkflowInstances' AND source_menu.del_flag=0
JOIN t_menu target ON target.name='WorkflowDiagram' AND target.del_flag=0
JOIN t_role_permission rp ON rp.role_id=rm.role_id AND rp.del_flag=0
JOIN t_permission p ON p.id=rp.permission_id AND p.code='workflow:approval:start:api' AND p.del_flag=0
WHERE rm.del_flag=0 AND NOT EXISTS (
    SELECT 1 FROM t_role_menu existing WHERE existing.role_id=rm.role_id AND existing.menu_id=target.id
);
