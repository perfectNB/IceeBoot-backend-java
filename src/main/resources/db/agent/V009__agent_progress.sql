-- 展示过程独立于正文增量，按根执行和序号排序，读取时校验会话归属。
CREATE TABLE IF NOT EXISTS t_agent_progress (
    root_run_id VARCHAR(64) NOT NULL,
    sequence_no BIGINT NOT NULL,
    payload LONGTEXT NOT NULL,
    PRIMARY KEY (root_run_id, sequence_no)
);
