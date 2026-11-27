INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '停用或启用审批模板','workflow:approval:intake:api','api',id,'/v1/workflow/approvals/drafts/{code}/intake','PUT','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:definition' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:intake:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:intake:api' AND d.del_flag=0
WHERE p.code IN ('workflow:approval:publish:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);
