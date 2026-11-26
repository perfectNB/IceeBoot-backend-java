ALTER TABLE t_agent_model_call ADD COLUMN provider VARCHAR(40) DEFAULT NULL;
ALTER TABLE t_agent_model_call ADD COLUMN cache_write_tokens BIGINT DEFAULT NULL;
ALTER TABLE t_agent_model_call ADD COLUMN estimated_cost_cny DECIMAL(22,10) DEFAULT NULL;
ALTER TABLE t_agent_model_call ADD INDEX idx_agent_call_usage_time (create_time);
