INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看支付记录详情','pay:record:detail:api','api',id,'/v1/pay/records/{id}','GET','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='pay:record' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='pay:record:detail:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='pay:record:detail:api' AND d.del_flag=0
WHERE p.code='pay:record:list:api' AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '更新支付配置状态','pay:config:status:api','api',id,'/v1/pay/configs/{id}/status','PUT','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='pay:config' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='pay:config:status:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='pay:config:status:api' AND d.del_flag=0
WHERE p.code='pay:config:update:api' AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);
