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
    cancel_requested BOOLEAN NOT NULL DEFAULT FALSE,
    heartbeat_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_code VARCHAR(64) NOT NULL DEFAULT '',
    trace_id VARCHAR(64) NOT NULL DEFAULT '',
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

CREATE TABLE IF NOT EXISTS t_agent_usage_quota (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id VARCHAR(64) NOT NULL,
 usage_day DATE NOT NULL,
 used_requests BIGINT NOT NULL DEFAULT 0,
 create_by VARCHAR(64), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_by VARCHAR(64), update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 del_flag TINYINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_agent_daily_quota(user_id,usage_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent_session (
 agent_id BIGINT NOT NULL, user_id VARCHAR(64) NOT NULL, session_id VARCHAR(100) NOT NULL,
 release_id VARCHAR(64) NOT NULL, version INT NOT NULL, title VARCHAR(120) NOT NULL DEFAULT '',
 owner VARCHAR(64) NOT NULL DEFAULT '', lease_until BIGINT NOT NULL DEFAULT 0,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(agent_id,user_id,session_id), KEY idx_agent_session_user(agent_id,user_id,update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS t_agent_message (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, agent_id BIGINT NOT NULL, user_id VARCHAR(64) NOT NULL,
 session_id VARCHAR(100) NOT NULL, run_id VARCHAR(64) NOT NULL, role VARCHAR(16) NOT NULL,
 content LONGTEXT NOT NULL, status VARCHAR(24) NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_agent_message_run(run_id,role), KEY idx_agent_message_session(agent_id,user_id,session_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS t_agent_session_state (
 agent_id BIGINT NOT NULL, user_id VARCHAR(64) NOT NULL, session_id VARCHAR(100) NOT NULL,
 storage_path VARCHAR(255) NOT NULL, revision BIGINT NOT NULL, payload LONGBLOB NOT NULL,
 PRIMARY KEY(agent_id,user_id,session_id,storage_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
