-- 普通角色通过现有授权页分别分配菜单和操作权限。
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '长期记忆','AgentMemory','/admin/agent-console/memory','AgentMemory','Notebook',p.id,9,'menu',0,0,1,'active','ai:memory:read:api',NOW(),NOW(),0
FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentMemory' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,status,sort,create_time,update_time,del_flag)
SELECT '长期记忆','ai:memory','menu',p.id,'active',9,NOW(),NOW(),0 FROM t_permission p WHERE p.code='ai' AND p.del_flag=0
AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '智能体选项','ai:memory:read-agents:api','api',p.id,'/v1/agent-console/memory/agents','GET','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:read-agents:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看画像策略','ai:memory:policy-read:api','api',p.id,'/v1/agent-console/memory/policies/{scope}/{agent}','GET','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:policy-read:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '配置画像策略','ai:memory:policy-write:api','api',p.id,'/v1/agent-console/memory/policies/{scope}/{agent}','PUT','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:policy-write:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看记忆','ai:memory:read:api','api',p.id,'/v1/agent-console/memory/profiles/{scope}/{agent}','GET','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:read:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '修改我的记忆','ai:memory:edit:api','api',p.id,'/v1/agent-console/memory/profiles/{scope}/{agent}/fields/{field}','PUT','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:edit:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '忘记我的记忆','ai:memory:forget:api','api',p.id,'/v1/agent-console/memory/profiles/{scope}/{agent}/fields/{field}','DELETE','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:forget:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '修改智能体公共经验','ai:memory:shared-edit:api','api',p.id,'/v1/agent-console/memory/shared/{agent}/fields/{field}','PUT','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:shared-edit:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '删除智能体公共经验','ai:memory:shared-forget:api','api',p.id,'/v1/agent-console/memory/shared/{agent}/fields/{field}','DELETE','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:shared-forget:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看我的共享授权','ai:memory:grant-read:api','api',p.id,'/v1/agent-console/memory/grants/{agent}','GET','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:grant-read:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '设置我的共享授权','ai:memory:grant-write:api','api',p.id,'/v1/agent-console/memory/grants/{agent}','PUT','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:grant-write:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看我的待确认记忆','ai:memory:review-read:api','api',p.id,'/v1/agent-console/memory/reviews/{scope}/{agent}','GET','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:review-read:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '确认我的记忆','ai:memory:review:api','api',p.id,'/v1/agent-console/memory/reviews/{scope}/{agent}/{id}','PUT','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:review:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看公共经验候选','ai:memory:shared-review-read:api','api',p.id,'/v1/agent-console/memory/shared/{agent}/reviews','GET','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:shared-review-read:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '审核公共经验','ai:memory:shared-review:api','api',p.id,'/v1/agent-console/memory/shared/{agent}/reviews/{id}','PUT','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:shared-review:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '查看平台通用画像策略','ai:memory:platform-policy-read:api','api',p.id,'/v1/agent-console/memory/platform-policy','GET','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:platform-policy-read:api' AND del_flag=0);

INSERT INTO t_permission(name,code,type,parent_id,path,method,status,sort,create_time,update_time,del_flag)
SELECT '配置平台通用画像策略','ai:memory:platform-policy-write:api','api',p.id,'/v1/agent-console/memory/platform-policy','PUT','active',0,NOW(),NOW(),0
FROM t_permission p WHERE p.code='ai:memory' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:memory:platform-policy-write:api' AND del_flag=0);

INSERT INTO t_role_menu(role_id,menu_id,create_time,update_time,del_flag)
SELECT r.id,m.id,NOW(),NOW(),0 FROM t_role r CROSS JOIN t_menu m WHERE r.code='superAdmin' AND r.status='active' AND r.del_flag=0 AND m.name='AgentMemory' AND m.del_flag=0
AND NOT EXISTS(SELECT 1 FROM t_role_menu x WHERE x.role_id=r.id AND x.menu_id=m.id AND x.del_flag=0);

INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT r.id,p.id,NOW(),NOW(),0 FROM t_role r CROSS JOIN t_permission p WHERE r.code='superAdmin' AND r.status='active' AND r.del_flag=0
AND (p.code='ai:memory' OR p.code LIKE 'ai:memory:%') AND p.del_flag=0
AND NOT EXISTS(SELECT 1 FROM t_role_permission x WHERE x.role_id=r.id AND x.permission_id=p.id AND x.del_flag=0);
