CREATE TABLE IF NOT EXISTS t_workflow_launch_role (
    blueprint_code VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (blueprint_code, version, role_code)
);
