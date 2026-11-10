-- 已部署 t_agent_* 规范化表的环境使用（MySQL 8）。新环境无需执行。
-- 停止旧节点、备份后执行；保留历史 code 和逻辑删除记录，仅限制有效记录的编码唯一性。
-- DDL 会隐式提交；每一步按当前元数据判断，可在部分成功后重新执行。


SET @agent_code_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='t_agent_capability' AND column_name='active_code') = 0,
    'ALTER TABLE t_agent_capability ADD COLUMN active_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN code ELSE NULL END)', 'SELECT 1');
PREPARE agent_code_statement FROM @agent_code_ddl;
EXECUTE agent_code_statement;
DEALLOCATE PREPARE agent_code_statement;


SET @agent_code_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_agent_capability' AND index_name='uk_t_agent_capability_active_code') = 0,
    'ALTER TABLE t_agent_capability ADD UNIQUE INDEX uk_t_agent_capability_active_code(active_code)', 'SELECT 1');
PREPARE agent_code_statement FROM @agent_code_ddl;
EXECUTE agent_code_statement;
DEALLOCATE PREPARE agent_code_statement;


SET @agent_code_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_agent_capability' AND index_name='uk_t_agent_capability_code') > 0,
    'ALTER TABLE t_agent_capability DROP INDEX uk_t_agent_capability_code', 'SELECT 1');
PREPARE agent_code_statement FROM @agent_code_ddl;
EXECUTE agent_code_statement;
DEALLOCATE PREPARE agent_code_statement;


SET @agent_code_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='t_agent_definition' AND column_name='active_code') = 0,
    'ALTER TABLE t_agent_definition ADD COLUMN active_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN code ELSE NULL END)', 'SELECT 1');
PREPARE agent_code_statement FROM @agent_code_ddl;
EXECUTE agent_code_statement;
DEALLOCATE PREPARE agent_code_statement;


SET @agent_code_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_agent_definition' AND index_name='uk_t_agent_definition_active_code') = 0,
    'ALTER TABLE t_agent_definition ADD UNIQUE INDEX uk_t_agent_definition_active_code(active_code)', 'SELECT 1');
PREPARE agent_code_statement FROM @agent_code_ddl;
EXECUTE agent_code_statement;
DEALLOCATE PREPARE agent_code_statement;


SET @agent_code_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_agent_definition' AND index_name='uk_t_agent_definition_code') > 0,
    'ALTER TABLE t_agent_definition DROP INDEX uk_t_agent_definition_code', 'SELECT 1');
PREPARE agent_code_statement FROM @agent_code_ddl;
EXECUTE agent_code_statement;
DEALLOCATE PREPARE agent_code_statement;
