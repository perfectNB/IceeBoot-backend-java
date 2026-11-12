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
