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
    cached_tokens BIGINT DEFAULT NULL,
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
    provider VARCHAR(40) DEFAULT NULL,
    cache_write_tokens BIGINT DEFAULT NULL,
    estimated_cost_cny DECIMAL(22,10) DEFAULT NULL,
    cached_tokens BIGINT DEFAULT NULL,
    usage_known BOOLEAN NOT NULL DEFAULT TRUE,
    create_by VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0,
    KEY idx_agent_model_call_root(root_run_id),
    KEY idx_agent_call_usage_time(create_time),
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

-- 展示过程独立于正文增量，按根执行和序号排序，读取时校验会话归属。
CREATE TABLE IF NOT EXISTS t_agent_progress (
    root_run_id VARCHAR(64) NOT NULL,
    sequence_no BIGINT NOT NULL,
    payload LONGTEXT NOT NULL,
    PRIMARY KEY (root_run_id, sequence_no)
);

-- 文件内容分块存储，避免受单条 SQL 最大包限制。文件归属不可由客户端修改。
CREATE TABLE IF NOT EXISTS t_agent_file (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    name VARCHAR(200) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_file_scope (agent_id,user_id,session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS t_agent_file_chunk (
    file_id VARCHAR(64) NOT NULL,
    part_no INT NOT NULL,
    payload MEDIUMBLOB NOT NULL,
    PRIMARY KEY(file_id,part_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS t_agent_run_file (
    run_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    purpose VARCHAR(16) NOT NULL DEFAULT 'INPUT',
    PRIMARY KEY(run_id,file_id),
    INDEX idx_agent_run_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS t_agent_memory_policy (
 id BIGINT NOT NULL AUTO_INCREMENT,
 scope VARCHAR(16) NOT NULL,
 agent_id BIGINT NOT NULL DEFAULT 0,
 prompt LONGTEXT NOT NULL,
 profile_schema TEXT NOT NULL,
 schema_version INT NOT NULL DEFAULT 1,
 model_config_id BIGINT NOT NULL,
 enabled TINYINT NOT NULL DEFAULT 0,
 review_required TINYINT NOT NULL DEFAULT 1,
 revision BIGINT NOT NULL DEFAULT 1,
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),
 UNIQUE KEY uk_memory_policy(scope,agent_id),
 CONSTRAINT ck_memory_policy_owner CHECK (
  (scope='USER' AND agent_id=0) OR (scope IN ('AGENT','AGENT_USER') AND agent_id>0)),
 CONSTRAINT ck_memory_policy_review CHECK (scope<>'AGENT' OR review_required=1),
 CONSTRAINT ck_memory_policy_revision CHECK (revision>0 AND schema_version>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 当前生效画像：加密文档包含 data、逐字段 evidence、过期时间与忘记标记。
CREATE TABLE IF NOT EXISTS t_agent_memory_profile (
 id BIGINT NOT NULL AUTO_INCREMENT,
 scope VARCHAR(16) NOT NULL,
 agent_id BIGINT NOT NULL DEFAULT 0,
 user_id VARCHAR(64) NOT NULL DEFAULT '',
 profile_ciphertext LONGTEXT NOT NULL,
 schema_version INT NOT NULL DEFAULT 1,
 revision BIGINT NOT NULL DEFAULT 1,
 forget_epoch BIGINT NOT NULL DEFAULT 0,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),
 UNIQUE KEY uk_memory_profile(scope,agent_id,user_id),
 KEY idx_memory_profile_user(user_id,scope,status),
 CONSTRAINT ck_memory_profile_owner CHECK (
  (scope='AGENT' AND agent_id>0 AND user_id='') OR
  (scope='AGENT_USER' AND agent_id>0 AND user_id<>'') OR
  (scope='USER' AND agent_id=0 AND user_id<>'')),
 CONSTRAINT ck_memory_profile_status CHECK (status IN ('ACTIVE','DISABLED')),
 CONSTRAINT ck_memory_profile_revision CHECK (revision>0 AND forget_epoch>=0 AND schema_version>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 与现有画像表分开存放的共享权限；空字段白名单表示不允许任何字段，而不是全部允许。
CREATE TABLE IF NOT EXISTS t_agent_memory_grant (
 id BIGINT NOT NULL AUTO_INCREMENT,
 user_id VARCHAR(64) NOT NULL,
 agent_id BIGINT NOT NULL,
 can_read TINYINT NOT NULL DEFAULT 0,
 can_propose_update TINYINT NOT NULL DEFAULT 0,
 read_fields TEXT NOT NULL,
 write_fields TEXT NOT NULL,
 revision BIGINT NOT NULL DEFAULT 1,
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),
 UNIQUE KEY uk_memory_grant(user_id,agent_id),
 CONSTRAINT ck_memory_grant_owner CHECK (user_id<>'' AND agent_id>0),
 CONSTRAINT ck_memory_grant_flags CHECK (can_read IN (0,1) AND can_propose_update IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每个根轮次一个可恢复任务。policy_snapshot_ciphertext 固定实际提示词、schema、模型配置版本。
-- request_ciphertext 固定本轮证据、目标画像 ID/基础 revision/forget_epoch，避免重试期间配置漂移。
CREATE TABLE IF NOT EXISTS t_agent_memory_update_job (
 id BIGINT NOT NULL AUTO_INCREMENT,
 source_run_id VARCHAR(64) NOT NULL,
 trigger_type VARCHAR(16) NOT NULL DEFAULT 'MODEL_TURN',
 execution_run_id VARCHAR(64) NOT NULL DEFAULT '',
 source_agent_id BIGINT NOT NULL,
 source_user_id VARCHAR(64) NOT NULL,
 source_session_id VARCHAR(100) NOT NULL,
 source_release_id VARCHAR(64) NOT NULL,
 policy_snapshot_ciphertext LONGTEXT NOT NULL,
 request_ciphertext LONGTEXT NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
 attempts INT NOT NULL DEFAULT 0,
 next_retry_at BIGINT NOT NULL DEFAULT 0,
 lease_owner VARCHAR(64) NOT NULL DEFAULT '',
 lease_until BIGINT NOT NULL DEFAULT 0,
 lease_epoch BIGINT NOT NULL DEFAULT 0,
 error_code VARCHAR(64) NOT NULL DEFAULT '',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),
 UNIQUE KEY uk_memory_job_run(source_run_id),
 KEY idx_memory_job_dispatch(status,next_retry_at,lease_until,id),
 KEY idx_memory_job_user(source_user_id,source_agent_id,id),
 CONSTRAINT ck_memory_job_status CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','RETRY','FAILED','CANCELLED')),
 CONSTRAINT ck_memory_job_owner CHECK (source_user_id<>'' AND (
  (trigger_type='MODEL_TURN' AND source_agent_id>0) OR
  (trigger_type IN ('USER_EDIT','ADMIN_EDIT') AND source_agent_id>=0))),
 CONSTRAINT ck_memory_job_attempts CHECK (attempts>=0 AND lease_epoch>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 一条候选对应一个画像目标，可包含多个字段 patch；审核后仍需重新验证版本、证据与忘记标记。
CREATE TABLE IF NOT EXISTS t_agent_memory_change (
 id BIGINT NOT NULL AUTO_INCREMENT,
 profile_id BIGINT NOT NULL,
 job_id BIGINT NOT NULL,
 base_revision BIGINT NOT NULL,
 applied_revision BIGINT NOT NULL DEFAULT 0,
 base_forget_epoch BIGINT NOT NULL DEFAULT 0,
 origin VARCHAR(16) NOT NULL DEFAULT 'MODEL',
 status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
 patch_ciphertext LONGTEXT NOT NULL,
 evidence_ciphertext LONGTEXT NOT NULL,
 actor_user_id VARCHAR(64) NOT NULL DEFAULT '',
 reviewer_user_id VARCHAR(64) NOT NULL DEFAULT '',
 reviewed_at BIGINT NOT NULL DEFAULT 0,
 error_code VARCHAR(64) NOT NULL DEFAULT '',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),
 UNIQUE KEY uk_memory_change_job(profile_id,job_id),
 KEY idx_memory_change_review(status,id),
 CONSTRAINT ck_memory_change_origin CHECK (origin IN ('MODEL','USER','ADMIN')),
 CONSTRAINT ck_memory_change_status CHECK (status IN ('PENDING','APPLIED','REJECTED','STALE','NO_CHANGE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
