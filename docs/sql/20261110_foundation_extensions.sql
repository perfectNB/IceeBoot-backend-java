-- 基础能力扩展：先在部署流程中核对实际结构；不要在已有列上重复执行 ALTER。
-- 已有数据不自动回填业务状态，不删除旧表、旧数据。全部金额字段沿用来源的元/分约定。
ALTER TABLE t_pay_record
    ADD COLUMN user_id bigint DEFAULT NULL COMMENT '用户ID',
    ADD COLUMN application_code varchar(255) DEFAULT NULL COMMENT '应用编码',
    ADD COLUMN business_type varchar(255) DEFAULT NULL COMMENT '支付用途编码',
    ADD COLUMN business_status varchar(255) DEFAULT NULL COMMENT '策略执行状态',
    ADD COLUMN business_retry_count int DEFAULT NULL COMMENT '失败次数',
    ADD COLUMN business_error varchar(255) DEFAULT NULL COMMENT '最近执行错误',
    ADD COLUMN description varchar(255) DEFAULT NULL COMMENT '订单描述',
    ADD COLUMN amount decimal(18,4) DEFAULT NULL COMMENT '订单金额，元',
    ADD COLUMN actual_amount decimal(18,4) DEFAULT NULL COMMENT '实际到账金额，元';

CREATE TABLE IF NOT EXISTS t_notification (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_code varchar(255) DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    content text,
    type varchar(255) DEFAULT NULL,
    target_user_id bigint DEFAULT NULL,
    status varchar(255) DEFAULT NULL,
    publish_time datetime DEFAULT NULL,
    expire_time datetime DEFAULT NULL,
    create_by varchar(64) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    update_by varchar(64) DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    del_flag tinyint DEFAULT 0,
    KEY idx_notification_audience (application_code, status, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_notification_read (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    notification_id bigint DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    read_time datetime DEFAULT NULL,
    create_by varchar(64) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    update_by varchar(64) DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    del_flag tinyint DEFAULT 0,
    UNIQUE KEY uk_notification_read (notification_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user_application (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id bigint DEFAULT NULL,
    application_code varchar(255) DEFAULT NULL,
    status varchar(255) DEFAULT NULL,
    first_login_time datetime DEFAULT NULL,
    last_login_time datetime DEFAULT NULL,
    create_by varchar(64) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    update_by varchar(64) DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    del_flag tinyint DEFAULT 0,
    UNIQUE KEY uk_user_application (user_id, application_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 存量索引单独核对后按需创建。下面两条查询仅检查，不自动修复重复数据。
SELECT out_trade_no, COUNT(*) FROM t_pay_record
WHERE out_trade_no IS NOT NULL GROUP BY out_trade_no HAVING COUNT(*) > 1;
-- 无重复且无同等索引时：
-- CREATE UNIQUE INDEX uk_pay_out_trade_no ON t_pay_record(out_trade_no);
-- CREATE INDEX idx_pay_compensation ON t_pay_record(pay_status, business_status, update_time);
-- 若三张新表已由实体初始化器创建，IF NOT EXISTS 不会补索引：按实际情况补建上述索引。
-- 框架的已读/进入应用操作还使用事务行锁，避免未建唯一索引时重复写入。
