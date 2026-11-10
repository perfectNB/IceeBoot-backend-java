-- AgentScope 控制台增量迁移。保留所有历史 AI 数据和 Activiti 审批表。
CREATE TABLE IF NOT EXISTS ai_model_config (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(120) NOT NULL,
 config_json LONGTEXT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_capability (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 kind VARCHAR(24) NOT NULL,
 code VARCHAR(64) NOT NULL,
 config_json LONGTEXT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_ai_capability_code(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_agent_definition (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(64) NOT NULL,
 name VARCHAR(120) NOT NULL,
 draft_json LONGTEXT NOT NULL,
 active_release_id VARCHAR(36) NOT NULL DEFAULT '',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_ai_agent_code(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_agent_release (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 agent_id BIGINT NOT NULL,
 version INT NOT NULL,
 snapshot_json LONGTEXT NOT NULL,
 created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_ai_release_version(agent_id,version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_agent_run (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 root_run_id VARCHAR(36) NOT NULL,
 parent_run_id VARCHAR(36) NOT NULL DEFAULT '',
 agent_id BIGINT NOT NULL,
 agent_code VARCHAR(64) NOT NULL,
 version INT NOT NULL,
 user_id VARCHAR(64) NOT NULL,
 status VARCHAR(24) NOT NULL,
 input_text LONGTEXT NOT NULL,
 output_text LONGTEXT NOT NULL,
 error_text VARCHAR(500) NOT NULL DEFAULT '',
 input_tokens BIGINT NOT NULL DEFAULT 0,
 output_tokens BIGINT NOT NULL DEFAULT 0,
 usage_known BOOLEAN NOT NULL DEFAULT TRUE,
 started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finished_at DATETIME NULL,
 KEY idx_ai_run_root(root_run_id),
 KEY idx_ai_run_agent(agent_id,started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_model_call (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 run_id VARCHAR(36) NOT NULL,
 root_run_id VARCHAR(36) NOT NULL,
 agent_code VARCHAR(64) NOT NULL,
 model VARCHAR(200) NOT NULL,
 input_text LONGTEXT NOT NULL,
 output_text LONGTEXT NOT NULL,
 input_tokens BIGINT NOT NULL DEFAULT 0,
 output_tokens BIGINT NOT NULL DEFAULT 0,
 usage_known BOOLEAN NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_ai_call_root(root_run_id),
 KEY idx_ai_call_run(run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 新控制台替代旧 Coze/Dify、Spring AI 演示菜单，历史表不删除。
UPDATE t_menu SET status='inactive',update_time=NOW() WHERE path='/admin/ai' OR component IN ('LLMManagement','AgentManagement','ChatInterface','RagChat');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag)
SELECT '智能体控制台','AgentConsoleRoot','/admin/agent-console','','Cpu',0,4,'menu',0,0,1,'active','ai:agent:read',NOW(),NOW(),0
WHERE NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentConsoleRoot');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT '智能体','HarnessAgents','/admin/agent-console/agents','HarnessAgents','Cpu',p.id,1,'menu',0,0,1,'active','ai:agent:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='HarnessAgents');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT '模型连接','ModelConnections','/admin/agent-console/models','ModelConnections','Cpu',p.id,2,'menu',0,0,1,'active','ai:model:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='ModelConnections');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT '技能','AgentSkills','/admin/agent-console/skills','CapabilityManagement','Cpu',p.id,3,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentSkills');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT '工具','AgentTools','/admin/agent-console/tools','CapabilityManagement','Cpu',p.id,4,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentTools');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT 'MCP 服务','AgentMcps','/admin/agent-console/mcps','CapabilityManagement','Cpu',p.id,5,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentMcps');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT '知识库连接','AgentKnowledge','/admin/agent-console/knowledge','CapabilityManagement','Cpu',p.id,6,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentKnowledge');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT '沙箱模板','AgentSandboxes','/admin/agent-console/sandboxes','CapabilityManagement','Cpu',p.id,7,'menu',0,0,1,'active','ai:capability:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentSandboxes');
INSERT INTO t_menu(title,name,path,component,icon,parent_id,sort,type,hidden,keep_alive,requires_auth,status,permissions,create_time,update_time,del_flag) SELECT '执行记录','AgentRuns','/admin/agent-console/runs','AgentRuns','Cpu',p.id,8,'menu',0,0,1,'active','ai:run:read',NOW(),NOW(),0 FROM t_menu p WHERE p.name='AgentConsoleRoot' AND NOT EXISTS(SELECT 1 FROM t_menu WHERE name='AgentRuns');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '模型列表','ai:model:read','api',0,'/v1/agent-console/models','GET','智能体控制台：模型列表','active',1,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:read' AND path='/v1/agent-console/models' AND method='GET');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '保存模型','ai:model:write','api',0,'/v1/agent-console/models','POST','智能体控制台：保存模型','active',2,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:write' AND path='/v1/agent-console/models' AND method='POST');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '删除模型','ai:model:write','api',0,'/v1/agent-console/models/{id}','DELETE','智能体控制台：删除模型','active',3,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:write' AND path='/v1/agent-console/models/{id}' AND method='DELETE');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '直接调用模型','ai:model:invoke','api',0,'/v1/agent-console/models/invoke','POST','智能体控制台：直接调用模型','active',4,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:model:invoke' AND path='/v1/agent-console/models/invoke' AND method='POST');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '能力列表','ai:capability:read','api',0,'/v1/agent-console/capabilities','GET','智能体控制台：能力列表','active',5,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:read' AND path='/v1/agent-console/capabilities' AND method='GET');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '保存能力','ai:capability:write','api',0,'/v1/agent-console/capabilities','POST','智能体控制台：保存能力','active',6,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities' AND method='POST');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '删除能力','ai:capability:write','api',0,'/v1/agent-console/capabilities/{id}','DELETE','智能体控制台：删除能力','active',7,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:capability:write' AND path='/v1/agent-console/capabilities/{id}' AND method='DELETE');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '智能体列表','ai:agent:read','api',0,'/v1/agent-console/agents','GET','智能体控制台：智能体列表','active',8,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:read' AND path='/v1/agent-console/agents' AND method='GET');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '智能体详情','ai:agent:read','api',0,'/v1/agent-console/agents/{id}','GET','智能体控制台：智能体详情','active',9,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:read' AND path='/v1/agent-console/agents/{id}' AND method='GET');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '保存智能体','ai:agent:write','api',0,'/v1/agent-console/agents','POST','智能体控制台：保存智能体','active',10,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:write' AND path='/v1/agent-console/agents' AND method='POST');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '删除智能体','ai:agent:write','api',0,'/v1/agent-console/agents/{id}','DELETE','智能体控制台：删除智能体','active',11,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:write' AND path='/v1/agent-console/agents/{id}' AND method='DELETE');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '版本列表','ai:agent:read','api',0,'/v1/agent-console/agents/{id}/releases','GET','智能体控制台：版本列表','active',12,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:read' AND path='/v1/agent-console/agents/{id}/releases' AND method='GET');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '发布智能体','ai:agent:publish','api',0,'/v1/agent-console/agents/{id}/publish','POST','智能体控制台：发布智能体','active',13,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:publish' AND path='/v1/agent-console/agents/{id}/publish' AND method='POST');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '调用智能体','ai:agent:invoke','api',0,'/v1/agent-console/agents/by-code/{code}/invoke','POST','智能体控制台：调用智能体','active',14,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:agent:invoke' AND path='/v1/agent-console/agents/by-code/{code}/invoke' AND method='POST');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '执行列表','ai:run:read','api',0,'/v1/agent-console/runs','GET','智能体控制台：执行列表','active',15,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:run:read' AND path='/v1/agent-console/runs' AND method='GET');
INSERT INTO t_permission(name,code,type,parent_id,path,method,description,status,sort,create_time,update_time,del_flag) SELECT '执行详情','ai:run:read','api',0,'/v1/agent-console/runs/{id}','GET','智能体控制台：执行详情','active',16,NOW(),NOW(),0 WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='ai:run:read' AND path='/v1/agent-console/runs/{id}' AND method='GET');

-- 新菜单和接口只关联超级管理员，不向其他角色开放。
INSERT INTO t_role_menu(role_id,menu_id,create_time,update_time,del_flag)
SELECT r.id,m.id,NOW(),NOW(),0 FROM t_role r JOIN t_menu m ON m.name IN ('AgentConsoleRoot','HarnessAgents','ModelConnections','AgentSkills','AgentTools','AgentMcps','AgentKnowledge','AgentSandboxes','AgentRuns')
WHERE r.code='superAdmin' AND r.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.del_flag=0);
INSERT INTO t_role_permission(role_id,permission_id,create_time,update_time,del_flag)
SELECT r.id,p.id,NOW(),NOW(),0 FROM t_role r JOIN t_permission p ON p.path LIKE '/v1/agent-console/%'
WHERE r.code='superAdmin' AND r.del_flag=0 AND NOT EXISTS(SELECT 1 FROM t_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id AND rp.del_flag=0);
