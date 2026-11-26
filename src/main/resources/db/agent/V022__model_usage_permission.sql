INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看智能体用量概览','ai:run:usage:api','api',p.id,'/v1/agent-console/usage','GET','active',0,NOW(),NOW(),0 FROM t_permission p
WHERE p.code='ai:run' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:run:usage:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT r.id,p.id,NOW(),NOW(),0 FROM t_role r CROSS JOIN t_permission p
WHERE r.code='superAdmin' AND r.del_flag=0 AND p.code='ai:run:usage:api' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id AND rp.del_flag=0);
