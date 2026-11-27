ALTER TABLE t_workflow_blueprint ADD COLUMN launch_role VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE t_workflow_blueprint ADD COLUMN published_name VARCHAR(120) NOT NULL DEFAULT '';
ALTER TABLE t_workflow_review_event ADD COLUMN node_id VARCHAR(128) NOT NULL DEFAULT '';
