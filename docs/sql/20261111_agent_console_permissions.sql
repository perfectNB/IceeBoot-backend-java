-- 智能体控制台菜单与动态接口权限（MySQL 8）。保留现有业务数据与普通角色授权。
-- 包含 9 个菜单和 44 个 HTTP 方法 + 路径映射；仅为超级管理员补齐本模块授权。
-- 可重复执行。规范化数据表由 agentscope_schema.sql / framework 迁移脚本建立。
START TRANSACTION;


INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '智能体控制台','AgentConsoleRoot','/admin/agent-console','','Cpu',0,4,'menu',0,0,1,'active','ai:agent:read',NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='AgentConsoleRoot' AND m.del_flag=0);

UPDATE t_menu SET title='智能体控制台',path='/admin/agent-console',component='',parent_id=0,status='active',permissions='ai:agent:read',update_time=NOW() WHERE name='AgentConsoleRoot' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '智能体','HarnessAgents','/admin/agent-console/agents','HarnessAgents','Cpu',p.id,1,'menu',0,0,1,'active','ai:agent:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='HarnessAgents' AND m.del_flag=0);

UPDATE t_menu SET title='智能体',path='/admin/agent-console/agents',component='HarnessAgents',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:agent:read',sort=1,update_time=NOW() WHERE name='HarnessAgents' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '模型连接','ModelConnections','/admin/agent-console/models','ModelConnections','Cpu',p.id,2,'menu',0,0,1,'active','ai:model:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='ModelConnections' AND m.del_flag=0);

UPDATE t_menu SET title='模型连接',path='/admin/agent-console/models',component='ModelConnections',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:model:read',sort=2,update_time=NOW() WHERE name='ModelConnections' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '技能','AgentSkills','/admin/agent-console/skills','CapabilityManagement','Cpu',p.id,3,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='AgentSkills' AND m.del_flag=0);

UPDATE t_menu SET title='技能',path='/admin/agent-console/skills',component='CapabilityManagement',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:capability:read',sort=3,update_time=NOW() WHERE name='AgentSkills' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '工具','AgentTools','/admin/agent-console/tools','CapabilityManagement','Cpu',p.id,4,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='AgentTools' AND m.del_flag=0);

UPDATE t_menu SET title='工具',path='/admin/agent-console/tools',component='CapabilityManagement',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:capability:read',sort=4,update_time=NOW() WHERE name='AgentTools' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT 'MCP 服务','AgentMcps','/admin/agent-console/mcps','CapabilityManagement','Cpu',p.id,5,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='AgentMcps' AND m.del_flag=0);

UPDATE t_menu SET title='MCP 服务',path='/admin/agent-console/mcps',component='CapabilityManagement',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:capability:read',sort=5,update_time=NOW() WHERE name='AgentMcps' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '知识库连接','AgentKnowledge','/admin/agent-console/knowledge','CapabilityManagement','Cpu',p.id,6,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='AgentKnowledge' AND m.del_flag=0);

UPDATE t_menu SET title='知识库连接',path='/admin/agent-console/knowledge',component='CapabilityManagement',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:capability:read',sort=6,update_time=NOW() WHERE name='AgentKnowledge' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '沙箱模板','AgentSandboxes','/admin/agent-console/sandboxes','CapabilityManagement','Cpu',p.id,7,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='AgentSandboxes' AND m.del_flag=0);

UPDATE t_menu SET title='沙箱模板',path='/admin/agent-console/sandboxes',component='CapabilityManagement',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:capability:read',sort=7,update_time=NOW() WHERE name='AgentSandboxes' AND del_flag=0;

INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '执行记录','AgentRuns','/admin/agent-console/runs','AgentRuns','Cpu',p.id,8,'menu',0,0,1,'active','ai:run:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND p.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_menu m WHERE m.name='AgentRuns' AND m.del_flag=0);

UPDATE t_menu SET title='执行记录',path='/admin/agent-console/runs',component='AgentRuns',parent_id=(SELECT id FROM (SELECT id FROM t_menu WHERE name='AgentConsoleRoot' AND del_flag=0 ORDER BY id LIMIT 1) root_menu),status='active',permissions='ai:run:read',sort=8,update_time=NOW() WHERE name='AgentRuns' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/models','ai:model:read','api',0,'/v1/agent-console/models','GET','智能体控制台：GET /v1/agent-console/models','active',1,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:read' AND path='/v1/agent-console/models' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:model:read' AND path='/v1/agent-console/models' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/models','ai:model:write','api',0,'/v1/agent-console/models','POST','智能体控制台：POST /v1/agent-console/models','active',2,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:write' AND path='/v1/agent-console/models' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:model:write' AND path='/v1/agent-console/models' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/models/{id}','ai:model:write','api',0,'/v1/agent-console/models/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/models/{id}','active',3,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:write' AND path='/v1/agent-console/models/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:model:write' AND path='/v1/agent-console/models/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/models/invoke','ai:model:invoke','api',0,'/v1/agent-console/models/invoke','POST','智能体控制台：POST /v1/agent-console/models/invoke','active',4,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:invoke' AND path='/v1/agent-console/models/invoke' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:model:invoke' AND path='/v1/agent-console/models/invoke' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/capabilities','ai:capability:read','api',0,'/v1/agent-console/capabilities','GET','智能体控制台：GET /v1/agent-console/capabilities','active',5,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/capabilities' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/capabilities' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/capabilities','ai:capability:write','api',0,'/v1/agent-console/capabilities','POST','智能体控制台：POST /v1/agent-console/capabilities','active',6,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/capabilities/{id}','ai:capability:write','api',0,'/v1/agent-console/capabilities/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/capabilities/{id}','active',7,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/agents','ai:agent:read','api',0,'/v1/agent-console/agents','GET','智能体控制台：GET /v1/agent-console/agents','active',8,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:read' AND path='/v1/agent-console/agents' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:read' AND path='/v1/agent-console/agents' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/agents/{id}','ai:agent:read','api',0,'/v1/agent-console/agents/{id}','GET','智能体控制台：GET /v1/agent-console/agents/{id}','active',9,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:read' AND path='/v1/agent-console/agents/{id}' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:read' AND path='/v1/agent-console/agents/{id}' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/agents','ai:agent:write','api',0,'/v1/agent-console/agents','POST','智能体控制台：POST /v1/agent-console/agents','active',10,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:write' AND path='/v1/agent-console/agents' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:write' AND path='/v1/agent-console/agents' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/agents/{id}','ai:agent:write','api',0,'/v1/agent-console/agents/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/agents/{id}','active',11,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:write' AND path='/v1/agent-console/agents/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:write' AND path='/v1/agent-console/agents/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/agents/{id}/releases','ai:agent:read','api',0,'/v1/agent-console/agents/{id}/releases','GET','智能体控制台：GET /v1/agent-console/agents/{id}/releases','active',12,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:read' AND path='/v1/agent-console/agents/{id}/releases' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:read' AND path='/v1/agent-console/agents/{id}/releases' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/agents/{id}/publish','ai:agent:publish','api',0,'/v1/agent-console/agents/{id}/publish','POST','智能体控制台：POST /v1/agent-console/agents/{id}/publish','active',13,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:publish' AND path='/v1/agent-console/agents/{id}/publish' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:publish' AND path='/v1/agent-console/agents/{id}/publish' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/agents/by-code/{code}/invoke','ai:agent:invoke','api',0,'/v1/agent-console/agents/by-code/{code}/invoke','POST','智能体控制台：POST /v1/agent-console/agents/by-code/{code}/invoke','active',14,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:invoke' AND path='/v1/agent-console/agents/by-code/{code}/invoke' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:invoke' AND path='/v1/agent-console/agents/by-code/{code}/invoke' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/runs','ai:run:read','api',0,'/v1/agent-console/runs','GET','智能体控制台：GET /v1/agent-console/runs','active',15,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:run:read' AND path='/v1/agent-console/runs' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:run:read' AND path='/v1/agent-console/runs' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/runs/{id}','ai:run:read','api',0,'/v1/agent-console/runs/{id}','GET','智能体控制台：GET /v1/agent-console/runs/{id}','active',16,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:run:read' AND path='/v1/agent-console/runs/{id}' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:run:read' AND path='/v1/agent-console/runs/{id}' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/models','ai:model:write','api',0,'/v1/agent-console/models','PUT','智能体控制台：PUT /v1/agent-console/models','active',17,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:write' AND path='/v1/agent-console/models' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:model:write' AND path='/v1/agent-console/models' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/agents','ai:agent:write','api',0,'/v1/agent-console/agents','PUT','智能体控制台：PUT /v1/agent-console/agents','active',18,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:write' AND path='/v1/agent-console/agents' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:agent:write' AND path='/v1/agent-console/agents' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/capabilities','ai:capability:write','api',0,'/v1/agent-console/capabilities','PUT','智能体控制台：PUT /v1/agent-console/capabilities','active',19,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/knowledge','ai:capability:read','api',0,'/v1/agent-console/knowledge','GET','智能体控制台：GET /v1/agent-console/knowledge','active',20,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/knowledge' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/knowledge' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/knowledge/{id}','ai:capability:read','api',0,'/v1/agent-console/knowledge/{id}','GET','智能体控制台：GET /v1/agent-console/knowledge/{id}','active',21,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/knowledge/{id}' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/knowledge/{id}' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/knowledge','ai:capability:write','api',0,'/v1/agent-console/knowledge','POST','智能体控制台：POST /v1/agent-console/knowledge','active',22,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/knowledge' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/knowledge' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/knowledge','ai:capability:write','api',0,'/v1/agent-console/knowledge','PUT','智能体控制台：PUT /v1/agent-console/knowledge','active',23,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/knowledge' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/knowledge' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/knowledge/{id}','ai:capability:write','api',0,'/v1/agent-console/knowledge/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/knowledge/{id}','active',24,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/knowledge/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/knowledge/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/mcps','ai:capability:read','api',0,'/v1/agent-console/mcps','GET','智能体控制台：GET /v1/agent-console/mcps','active',25,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/mcps' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/mcps' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/mcps/{id}','ai:capability:read','api',0,'/v1/agent-console/mcps/{id}','GET','智能体控制台：GET /v1/agent-console/mcps/{id}','active',26,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/mcps/{id}' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/mcps/{id}' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/mcps','ai:capability:write','api',0,'/v1/agent-console/mcps','POST','智能体控制台：POST /v1/agent-console/mcps','active',27,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/mcps' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/mcps' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/mcps','ai:capability:write','api',0,'/v1/agent-console/mcps','PUT','智能体控制台：PUT /v1/agent-console/mcps','active',28,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/mcps' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/mcps' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/mcps/{id}','ai:capability:write','api',0,'/v1/agent-console/mcps/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/mcps/{id}','active',29,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/mcps/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/mcps/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/sandbox-templates','ai:capability:read','api',0,'/v1/agent-console/sandbox-templates','GET','智能体控制台：GET /v1/agent-console/sandbox-templates','active',30,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/sandbox-templates' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/sandbox-templates' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/sandbox-templates/{id}','ai:capability:read','api',0,'/v1/agent-console/sandbox-templates/{id}','GET','智能体控制台：GET /v1/agent-console/sandbox-templates/{id}','active',31,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/sandbox-templates/{id}' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/sandbox-templates/{id}' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/sandbox-templates','ai:capability:write','api',0,'/v1/agent-console/sandbox-templates','POST','智能体控制台：POST /v1/agent-console/sandbox-templates','active',32,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/sandbox-templates' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/sandbox-templates' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/sandbox-templates','ai:capability:write','api',0,'/v1/agent-console/sandbox-templates','PUT','智能体控制台：PUT /v1/agent-console/sandbox-templates','active',33,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/sandbox-templates' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/sandbox-templates' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/sandbox-templates/{id}','ai:capability:write','api',0,'/v1/agent-console/sandbox-templates/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/sandbox-templates/{id}','active',34,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/sandbox-templates/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/sandbox-templates/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/skills','ai:capability:read','api',0,'/v1/agent-console/skills','GET','智能体控制台：GET /v1/agent-console/skills','active',35,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/skills' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/skills' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/skills/{id}','ai:capability:read','api',0,'/v1/agent-console/skills/{id}','GET','智能体控制台：GET /v1/agent-console/skills/{id}','active',36,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/skills/{id}' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/skills/{id}' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/skills','ai:capability:write','api',0,'/v1/agent-console/skills','POST','智能体控制台：POST /v1/agent-console/skills','active',37,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/skills' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/skills' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/skills','ai:capability:write','api',0,'/v1/agent-console/skills','PUT','智能体控制台：PUT /v1/agent-console/skills','active',38,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/skills' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/skills' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/skills/{id}','ai:capability:write','api',0,'/v1/agent-console/skills/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/skills/{id}','active',39,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/skills/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/skills/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/tools','ai:capability:read','api',0,'/v1/agent-console/tools','GET','智能体控制台：GET /v1/agent-console/tools','active',40,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/tools' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/tools' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'GET /v1/agent-console/tools/{id}','ai:capability:read','api',0,'/v1/agent-console/tools/{id}','GET','智能体控制台：GET /v1/agent-console/tools/{id}','active',41,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/tools/{id}' AND method='GET' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:read' AND path='/v1/agent-console/tools/{id}' AND method='GET' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'POST /v1/agent-console/tools','ai:capability:write','api',0,'/v1/agent-console/tools','POST','智能体控制台：POST /v1/agent-console/tools','active',42,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/tools' AND method='POST' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/tools' AND method='POST' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'PUT /v1/agent-console/tools','ai:capability:write','api',0,'/v1/agent-console/tools','PUT','智能体控制台：PUT /v1/agent-console/tools','active',43,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/tools' AND method='PUT' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/tools' AND method='PUT' AND del_flag=0;

INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag)
SELECT 'DELETE /v1/agent-console/tools/{id}','ai:capability:write','api',0,'/v1/agent-console/tools/{id}','DELETE','智能体控制台：DELETE /v1/agent-console/tools/{id}','active',44,NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/tools/{id}' AND method='DELETE' AND del_flag=0);

UPDATE t_permission SET status='active',update_time=NOW() WHERE code='ai:capability:write' AND path='/v1/agent-console/tools/{id}' AND method='DELETE' AND del_flag=0;

INSERT INTO t_role_menu(role_id,menu_id,create_time,update_time,del_flag)
SELECT r.id,m.id,NOW(),NOW(),0 FROM t_role r JOIN t_menu m ON m.name IN ('AgentConsoleRoot','HarnessAgents','ModelConnections','AgentSkills','AgentTools','AgentMcps','AgentKnowledge','AgentSandboxes','AgentRuns') AND m.del_flag=0 AND m.status='active'
WHERE r.code='superAdmin' AND r.del_flag=0 AND r.status='active' AND NOT EXISTS(SELECT 1 FROM t_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT r.id,p.id,NOW(),NOW(),0 FROM t_role r JOIN t_permission p ON (
    (p.method='GET' AND p.path='/v1/agent-console/models' AND p.code='ai:model:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/models' AND p.code='ai:model:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/models/{id}' AND p.code='ai:model:write')
 OR (p.method='POST' AND p.path='/v1/agent-console/models/invoke' AND p.code='ai:model:invoke')
 OR (p.method='GET' AND p.path='/v1/agent-console/capabilities' AND p.code='ai:capability:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/capabilities' AND p.code='ai:capability:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/capabilities/{id}' AND p.code='ai:capability:write')
 OR (p.method='GET' AND p.path='/v1/agent-console/agents' AND p.code='ai:agent:read')
 OR (p.method='GET' AND p.path='/v1/agent-console/agents/{id}' AND p.code='ai:agent:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/agents' AND p.code='ai:agent:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/agents/{id}' AND p.code='ai:agent:write')
 OR (p.method='GET' AND p.path='/v1/agent-console/agents/{id}/releases' AND p.code='ai:agent:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/agents/{id}/publish' AND p.code='ai:agent:publish')
 OR (p.method='POST' AND p.path='/v1/agent-console/agents/by-code/{code}/invoke' AND p.code='ai:agent:invoke')
 OR (p.method='GET' AND p.path='/v1/agent-console/runs' AND p.code='ai:run:read')
 OR (p.method='GET' AND p.path='/v1/agent-console/runs/{id}' AND p.code='ai:run:read')
 OR (p.method='PUT' AND p.path='/v1/agent-console/models' AND p.code='ai:model:write')
 OR (p.method='PUT' AND p.path='/v1/agent-console/agents' AND p.code='ai:agent:write')
 OR (p.method='PUT' AND p.path='/v1/agent-console/capabilities' AND p.code='ai:capability:write')
 OR (p.method='GET' AND p.path='/v1/agent-console/knowledge' AND p.code='ai:capability:read')
 OR (p.method='GET' AND p.path='/v1/agent-console/knowledge/{id}' AND p.code='ai:capability:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/knowledge' AND p.code='ai:capability:write')
 OR (p.method='PUT' AND p.path='/v1/agent-console/knowledge' AND p.code='ai:capability:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/knowledge/{id}' AND p.code='ai:capability:write')
 OR (p.method='GET' AND p.path='/v1/agent-console/mcps' AND p.code='ai:capability:read')
 OR (p.method='GET' AND p.path='/v1/agent-console/mcps/{id}' AND p.code='ai:capability:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/mcps' AND p.code='ai:capability:write')
 OR (p.method='PUT' AND p.path='/v1/agent-console/mcps' AND p.code='ai:capability:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/mcps/{id}' AND p.code='ai:capability:write')
 OR (p.method='GET' AND p.path='/v1/agent-console/sandbox-templates' AND p.code='ai:capability:read')
 OR (p.method='GET' AND p.path='/v1/agent-console/sandbox-templates/{id}' AND p.code='ai:capability:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/sandbox-templates' AND p.code='ai:capability:write')
 OR (p.method='PUT' AND p.path='/v1/agent-console/sandbox-templates' AND p.code='ai:capability:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/sandbox-templates/{id}' AND p.code='ai:capability:write')
 OR (p.method='GET' AND p.path='/v1/agent-console/skills' AND p.code='ai:capability:read')
 OR (p.method='GET' AND p.path='/v1/agent-console/skills/{id}' AND p.code='ai:capability:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/skills' AND p.code='ai:capability:write')
 OR (p.method='PUT' AND p.path='/v1/agent-console/skills' AND p.code='ai:capability:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/skills/{id}' AND p.code='ai:capability:write')
 OR (p.method='GET' AND p.path='/v1/agent-console/tools' AND p.code='ai:capability:read')
 OR (p.method='GET' AND p.path='/v1/agent-console/tools/{id}' AND p.code='ai:capability:read')
 OR (p.method='POST' AND p.path='/v1/agent-console/tools' AND p.code='ai:capability:write')
 OR (p.method='PUT' AND p.path='/v1/agent-console/tools' AND p.code='ai:capability:write')
 OR (p.method='DELETE' AND p.path='/v1/agent-console/tools/{id}' AND p.code='ai:capability:write')
) AND p.del_flag=0 AND p.status='active'
WHERE r.code='superAdmin' AND r.del_flag=0 AND r.status='active' AND NOT EXISTS(SELECT 1 FROM t_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id AND rp.del_flag=0);
COMMIT;
