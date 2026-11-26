CREATE TABLE IF NOT EXISTS t_workflow_blueprint (
    code VARCHAR(64) PRIMARY KEY,
    revision INT NOT NULL,
    version INT NOT NULL,
    draft MEDIUMTEXT NOT NULL,
    published MEDIUMTEXT
);
CREATE TABLE IF NOT EXISTS t_workflow_application (
    id VARCHAR(36) PRIMARY KEY,
    blueprint_code VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    blueprint MEDIUMTEXT NOT NULL,
    title VARCHAR(120) NOT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    round_no INT NOT NULL,
    revision INT NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    role_code VARCHAR(128) NOT NULL,
    form_data MEDIUMTEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_workflow_owner (owner_id, updated_at),
    INDEX idx_workflow_pending (status, role_code, updated_at)
);
CREATE TABLE IF NOT EXISTS t_workflow_review_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id VARCHAR(36) NOT NULL,
    round_no INT NOT NULL,
    action VARCHAR(16) NOT NULL,
    node_name VARCHAR(120) NOT NULL,
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(128) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    submitted_form MEDIUMTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_workflow_event_case (application_id, id),
    INDEX idx_workflow_event_actor (actor_id, application_id)
);
