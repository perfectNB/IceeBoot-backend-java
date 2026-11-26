INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '审批草稿列表','workflow:approval:drafts:api','api',id,'/v1/workflow/approvals/drafts','GET','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:definition' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:drafts:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:drafts:api' AND d.del_flag=0
WHERE p.code IN ('workflow:definition:deploy:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '审批角色选项','workflow:approval:roles:api','api',id,'/v1/workflow/approvals/roles','GET','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:definition' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:roles:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:roles:api' AND d.del_flag=0
WHERE p.code IN ('workflow:definition:deploy:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '保存审批草稿','workflow:approval:save:api','api',id,'/v1/workflow/approvals/drafts','PUT','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:definition' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:save:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:save:api' AND d.del_flag=0
WHERE p.code IN ('workflow:definition:deploy:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '发布审批模板','workflow:approval:publish:api','api',id,'/v1/workflow/approvals/drafts/{code}/publish','POST','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:definition' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:publish:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:publish:api' AND d.del_flag=0
WHERE p.code IN ('workflow:definition:deploy:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '选择已发布审批','workflow:approval:published:api','api',id,'/v1/workflow/approvals/published','GET','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:process' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:published:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:published:api' AND d.del_flag=0
WHERE p.code IN ('workflow:process:start:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '提交审批申请','workflow:approval:start:api','api',id,'/v1/workflow/approvals/cases','POST','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:process' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:start:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:start:api' AND d.del_flag=0
WHERE p.code IN ('workflow:process:start:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看个人审批列表','workflow:approval:list:api','api',id,'/v1/workflow/approvals/cases','GET','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:task' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:list:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:list:api' AND d.del_flag=0
WHERE p.code IN ('workflow:task:my:api','workflow:process:start:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看审批轨迹','workflow:approval:detail:api','api',id,'/v1/workflow/approvals/cases/{id}','GET','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:task' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:detail:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:detail:api' AND d.del_flag=0
WHERE p.code IN ('workflow:task:my:api','workflow:process:start:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '处理角色审批','workflow:approval:review:api','api',id,'/v1/workflow/approvals/cases/{id}/review/{action}','POST','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:task' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:review:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:review:api' AND d.del_flag=0
WHERE p.code IN ('workflow:task:complete:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '重提或作废申请','workflow:approval:revise:api','api',id,'/v1/workflow/approvals/cases/{id}/owner/{action}','POST','active',0,NOW(),NOW(),0
FROM t_permission WHERE code='workflow:process' AND del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_permission WHERE code='workflow:approval:revise:api' AND del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT DISTINCT rp.role_id,d.id,NOW(),NOW(),0 FROM t_role_permission rp JOIN t_permission p ON p.id=rp.permission_id
JOIN t_permission d ON d.code='workflow:approval:revise:api' AND d.del_flag=0
WHERE p.code IN ('workflow:process:start:api') AND p.del_flag=0 AND rp.del_flag=0 AND NOT EXISTS
(SELECT 1 FROM t_role_permission x WHERE x.role_id=rp.role_id AND x.permission_id=d.id AND x.del_flag=0);
