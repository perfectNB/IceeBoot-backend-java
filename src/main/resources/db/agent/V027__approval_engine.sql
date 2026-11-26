CREATE TABLE IF NOT EXISTS t_workflow_engine_definition (
    blueprint_code VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    definition_id VARCHAR(128) NOT NULL UNIQUE,
    PRIMARY KEY (blueprint_code,version)
);
CREATE TABLE IF NOT EXISTS t_workflow_engine_round (
    application_id VARCHAR(36) NOT NULL,
    round_no INT NOT NULL,
    instance_id VARCHAR(64) NOT NULL UNIQUE,
    PRIMARY KEY (application_id,round_no)
);
ALTER TABLE t_workflow_review_event ADD COLUMN review_role VARCHAR(128) NOT NULL DEFAULT '';
