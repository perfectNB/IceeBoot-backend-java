ALTER TABLE t_agent_capability ADD COLUMN active_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN code ELSE NULL END);
ALTER TABLE t_agent_capability ADD UNIQUE INDEX uk_t_agent_capability_active_code(active_code);
ALTER TABLE t_agent_capability DROP INDEX uk_t_agent_capability_code;
ALTER TABLE t_agent_definition ADD COLUMN active_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN code ELSE NULL END);
ALTER TABLE t_agent_definition ADD UNIQUE INDEX uk_t_agent_definition_active_code(active_code);
ALTER TABLE t_agent_definition DROP INDEX uk_t_agent_definition_code;
