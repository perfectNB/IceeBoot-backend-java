CREATE TABLE IF NOT EXISTS t_pay_order_terms (
    record_id BIGINT NOT NULL PRIMARY KEY,
    channel VARCHAR(24) NOT NULL,
    pay_code VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS t_pay_entitlement (
    record_id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pay_code VARCHAR(255) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entitlement_user_code (user_id, pay_code)
);
