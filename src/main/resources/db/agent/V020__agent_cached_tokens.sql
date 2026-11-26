ALTER TABLE t_agent_model_call ADD COLUMN cached_tokens BIGINT DEFAULT NULL;
ALTER TABLE t_agent_run ADD COLUMN cached_tokens BIGINT DEFAULT NULL;
