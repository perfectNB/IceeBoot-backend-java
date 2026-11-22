-- UTC 毫秒时间避免数据库会话时区影响；不存储登录令牌。
CREATE TABLE IF NOT EXISTS t_agent_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    name VARCHAR(80) NOT NULL,
    input MEDIUMTEXT NOT NULL,
    schedule_type VARCHAR(8) NOT NULL,
    cron_expression VARCHAR(120),
    zone_id VARCHAR(64) NOT NULL,
    execute_at BIGINT,
    next_fire_at BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    running_id VARCHAR(64),
    lease_until BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_schedule_due (enabled,deleted,next_fire_at),
    INDEX idx_agent_schedule_owner (owner_id,deleted,id),
    INDEX idx_agent_schedule_lease (lease_until,running_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS t_agent_schedule_execution (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    input MEDIUMTEXT NOT NULL,
    scheduled_at BIGINT NOT NULL,
    started_at BIGINT NOT NULL,
    finished_at BIGINT,
    status VARCHAR(24) NOT NULL,
    message VARCHAR(240) NOT NULL DEFAULT '',
    run_id VARCHAR(64),
    session_id VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_agent_schedule_occurrence (schedule_id,scheduled_at),
    INDEX idx_agent_schedule_history (schedule_id,owner_id,scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
