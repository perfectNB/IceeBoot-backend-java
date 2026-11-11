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
