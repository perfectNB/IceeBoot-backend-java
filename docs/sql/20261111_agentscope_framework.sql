-- AgentScope framework 分层重构迁移（MySQL 8）。
-- 停止旧节点并备份后执行。保留 ai_* 原表、原主键与发布快照；不连接或修改其他业务表。
-- 先创建目标表，再按 ID 复制缺失记录。重复执行不会覆盖目标表中已经编辑的数据。
-- 新环境直接执行 src/main/resources/sql/agentscope_schema.sql，无需运行下列旧表复制语句。

-- AgentScope 规范化表结构。先于应用启动执行；新建环境使用，历史数据迁移见 docs/sql/20261111_agentscope_framework.sql。

CREATE TABLE IF NOT EXISTS t_agent_model (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    provider VARCHAR(24) NOT NULL,
    protocol VARCHAR(24) NOT NULL,
    base_url VARCHAR(1000) NOT NULL,
    api_key TEXT NOT NULL,
    model VARCHAR(200) NOT NULL,
    context_window INT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    generation_json LONGTEXT,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_capability (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    kind VARCHAR(24) NOT NULL,
    code VARCHAR(64) NOT NULL,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0,
    active_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN code ELSE NULL END),
    UNIQUE KEY uk_t_agent_capability_active_code(active_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_skill (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    content LONGTEXT NOT NULL,
    resources_json LONGTEXT,
    tool_ids_json LONGTEXT,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_tool (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    kind VARCHAR(24) NOT NULL,
    url VARCHAR(1000),
    method VARCHAR(12),
    headers_json LONGTEXT,
    input_schema_json LONGTEXT NOT NULL,
    script LONGTEXT,
    project_api BOOLEAN NOT NULL DEFAULT FALSE,
    permission_code VARCHAR(120),
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_mcp (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    url VARCHAR(1000) NOT NULL,
    headers_json LONGTEXT,
    allowed_tools_json LONGTEXT NOT NULL,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_sandbox_template (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    template_id VARCHAR(200) NOT NULL,
    timeout_seconds INT,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_knowledge (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    url VARCHAR(1000) NOT NULL,
    method VARCHAR(12) NOT NULL,
    headers_json LONGTEXT,
    input_schema_json LONGTEXT NOT NULL,
    project_api BOOLEAN NOT NULL DEFAULT FALSE,
    permission_code VARCHAR(120),
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_definition (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    avatar VARCHAR(1000),
    prompt LONGTEXT NOT NULL,
    model_config_id BIGINT NOT NULL,
    max_rounds INT NOT NULL,
    sandbox_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sandbox_template_id BIGINT,
    plan_mode BOOLEAN NOT NULL DEFAULT FALSE,
    output_format VARCHAR(24) NOT NULL,
    output_schema_json LONGTEXT,
    generation_json LONGTEXT,
    skill_ids_json LONGTEXT,
    tool_ids_json LONGTEXT,
    mcp_ids_json LONGTEXT,
    knowledge_ids_json LONGTEXT,
    child_agent_ids_json LONGTEXT,
    environment_json LONGTEXT,
    active_release_id VARCHAR(36) NOT NULL DEFAULT '',
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0,
    active_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN code ELSE NULL END),
    UNIQUE KEY uk_t_agent_definition_active_code(active_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_release (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    version INT NOT NULL,
    snapshot_json LONGTEXT NOT NULL,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_agent_release_version(agent_id,version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_run (
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
    finished_at DATETIME,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0,
    KEY idx_agent_run_root(root_run_id),
    KEY idx_agent_run_agent(agent_id,started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_model_call (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    root_run_id VARCHAR(36) NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    model VARCHAR(200) NOT NULL,
    input_text LONGTEXT NOT NULL,
    output_text LONGTEXT NOT NULL,
    input_tokens BIGINT NOT NULL,
    output_tokens BIGINT NOT NULL,
    usage_known BOOLEAN NOT NULL DEFAULT TRUE,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0,
    KEY idx_agent_model_call_root(root_run_id),
    KEY idx_agent_model_call_run(run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

START TRANSACTION;

INSERT INTO t_agent_model (id, name, provider, protocol, base_url, api_key, model, context_window, enabled, generation_json, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.name')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.provider')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.protocol')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.baseUrl')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.apiKey')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.model')), 'null'), CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.contextWindow')), 'null') AS SIGNED), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.enabled')), 'null'), 'false') = 'true', JSON_EXTRACT(s.config_json, '$.generation'), NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_model_config s
WHERE NOT EXISTS (SELECT 1 FROM t_agent_model t WHERE t.id=s.id);

INSERT INTO t_agent_capability (id, kind, code, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, s.kind, s.code, NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_capability s
WHERE NOT EXISTS (SELECT 1 FROM t_agent_capability t WHERE t.id=s.id);

INSERT INTO t_agent_skill (id, code, name, description, enabled, content, resources_json, tool_ids_json, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.code')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.name')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.description')), 'null'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.enabled')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.skill.content')), 'null'), JSON_EXTRACT(s.config_json, '$.skill.resources'), JSON_EXTRACT(s.config_json, '$.skill.toolIds'), NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_capability s
WHERE s.kind='SKILL' AND NOT EXISTS (SELECT 1 FROM t_agent_skill t WHERE t.id=s.id);

INSERT INTO t_agent_tool (id, code, name, description, enabled, kind, url, method, headers_json, input_schema_json, script, project_api, permission_code, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.code')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.name')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.description')), 'null'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.enabled')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.kind')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.url')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.method')), 'null'), JSON_EXTRACT(s.config_json, '$.tool.headers'), JSON_EXTRACT(s.config_json, '$.tool.inputSchema'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.script')), 'null'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.projectApi')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.permissionCode')), 'null'), NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_capability s
WHERE s.kind='TOOL' AND NOT EXISTS (SELECT 1 FROM t_agent_tool t WHERE t.id=s.id);

INSERT INTO t_agent_mcp (id, code, name, description, enabled, url, headers_json, allowed_tools_json, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.code')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.name')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.description')), 'null'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.enabled')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.mcp.url')), 'null'), JSON_EXTRACT(s.config_json, '$.mcp.headers'), JSON_EXTRACT(s.config_json, '$.mcp.allowedTools'), NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_capability s
WHERE s.kind='MCP' AND NOT EXISTS (SELECT 1 FROM t_agent_mcp t WHERE t.id=s.id);

INSERT INTO t_agent_sandbox_template (id, code, name, description, enabled, template_id, timeout_seconds, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.code')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.name')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.description')), 'null'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.enabled')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.sandbox.templateId')), 'null'), CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.sandbox.timeoutSeconds')), 'null') AS SIGNED), NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_capability s
WHERE s.kind='SANDBOX' AND NOT EXISTS (SELECT 1 FROM t_agent_sandbox_template t WHERE t.id=s.id);

INSERT INTO t_agent_knowledge (id, code, name, description, enabled, url, method, headers_json, input_schema_json, project_api, permission_code, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.code')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.name')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.description')), 'null'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.enabled')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.url')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.method')), 'null'), JSON_EXTRACT(s.config_json, '$.tool.headers'), JSON_EXTRACT(s.config_json, '$.tool.inputSchema'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.projectApi')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.config_json, '$.tool.permissionCode')), 'null'), NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_capability s
WHERE s.kind='KNOWLEDGE' AND NOT EXISTS (SELECT 1 FROM t_agent_knowledge t WHERE t.id=s.id);

INSERT INTO t_agent_definition (id, code, name, description, enabled, avatar, prompt, model_config_id, max_rounds, sandbox_enabled, sandbox_template_id, plan_mode, output_format, output_schema_json, generation_json, skill_ids_json, tool_ids_json, mcp_ids_json, knowledge_ids_json, child_agent_ids_json, environment_json, active_release_id, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.code')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.name')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.description')), 'null'), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.enabled')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.avatar')), 'null'), NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.prompt')), 'null'), CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.modelConfigId')), 'null') AS SIGNED), CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.maxRounds')), 'null') AS SIGNED), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.sandboxEnabled')), 'null'), 'false') = 'true', CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.sandboxTemplateId')), 'null') AS SIGNED), COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.planMode')), 'null'), 'false') = 'true', NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.draft_json, '$.outputFormat')), 'null'), JSON_EXTRACT(s.draft_json, '$.outputSchema'), JSON_EXTRACT(s.draft_json, '$.generation'), JSON_EXTRACT(s.draft_json, '$.skillIds'), JSON_EXTRACT(s.draft_json, '$.toolIds'), JSON_EXTRACT(s.draft_json, '$.mcpIds'), JSON_EXTRACT(s.draft_json, '$.knowledgeIds'), JSON_EXTRACT(s.draft_json, '$.childAgentIds'), JSON_EXTRACT(s.draft_json, '$.environment'), s.active_release_id, NULL, s.created_at, NULL, s.updated_at, 0
FROM ai_agent_definition s
WHERE NOT EXISTS (SELECT 1 FROM t_agent_definition t WHERE t.id=s.id);

INSERT INTO t_agent_release (id, agent_id, version, snapshot_json, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, s.agent_id, s.version, s.snapshot_json, s.created_by, s.created_at, s.created_by, s.created_at, 0
FROM ai_agent_release s
WHERE NOT EXISTS (SELECT 1 FROM t_agent_release t WHERE t.id=s.id);

INSERT INTO t_agent_run (id, root_run_id, parent_run_id, agent_id, agent_code, version, user_id, status, input_text, output_text, error_text, input_tokens, output_tokens, usage_known, started_at, finished_at, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, s.root_run_id, s.parent_run_id, s.agent_id, s.agent_code, s.version, s.user_id, s.status, s.input_text, s.output_text, s.error_text, s.input_tokens, s.output_tokens, s.usage_known, s.started_at, s.finished_at, NULL, s.started_at, NULL, COALESCE(s.finished_at,s.started_at), 0
FROM ai_agent_run s
WHERE NOT EXISTS (SELECT 1 FROM t_agent_run t WHERE t.id=s.id);

INSERT INTO t_agent_model_call (id, run_id, root_run_id, agent_code, model, input_text, output_text, input_tokens, output_tokens, usage_known, create_by, create_time, update_by, update_time, del_flag)
SELECT s.id, s.run_id, s.root_run_id, s.agent_code, s.model, s.input_text, s.output_text, s.input_tokens, s.output_tokens, s.usage_known, NULL, s.created_at, NULL, s.created_at, 0
FROM ai_model_call s
WHERE NOT EXISTS (SELECT 1 FROM t_agent_model_call t WHERE t.id=s.id);

COMMIT;

-- 验收：对照 ai_* 与 t_agent_* 的数量、主键及快照；确认后部署新代码。
