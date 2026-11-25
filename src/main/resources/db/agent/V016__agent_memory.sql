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
