-- P0-P3 数据库初始化脚本（MySQL 8）
-- 执行: mysql -uroot -p < deploy/sql/schema.sql

CREATE DATABASE IF NOT EXISTS carpool_passenger_db DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS carpool_trip_db DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS carpool_driver_db DEFAULT CHARACTER SET utf8mb4;

USE carpool_passenger_db;

CREATE TABLE IF NOT EXISTS t_passenger (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL UNIQUE,
    nickname     VARCHAR(64)  NULL COMMENT '昵称',
    avatar_object VARCHAR(512) NULL COMMENT 'MinIO 头像 object key',
    password     VARCHAR(128) NOT NULL,
    credit_score INT          NOT NULL DEFAULT 80,
    balance      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '乘客';

CREATE TABLE IF NOT EXISTS t_passenger_status (
    passenger_id BIGINT PRIMARY KEY,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/BANNED'
) COMMENT '乘客状态（封禁）';

INSERT INTO t_passenger (id, username, password, credit_score, balance) VALUES
    (1, 'passenger1', '123456', 85, 500.00),
    (2, 'passenger2', '123456', 85, 50.00),
    (3, 'passenger3', '123456', 50, 500.00)
ON DUPLICATE KEY UPDATE username=username;

INSERT INTO t_passenger_status (passenger_id, status) VALUES
    (1, 'ACTIVE'), (2, 'ACTIVE'), (3, 'ACTIVE')
ON DUPLICATE KEY UPDATE status=status;

CREATE TABLE IF NOT EXISTS undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(100) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'Seata AT 模式回滚日志';

USE carpool_driver_db;

CREATE TABLE IF NOT EXISTS t_driver (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    nickname      VARCHAR(64)  NULL COMMENT '昵称',
    avatar_object VARCHAR(512) NULL COMMENT 'MinIO 头像 object key',
    password      VARCHAR(128) NOT NULL,
    real_name     VARCHAR(64)  NOT NULL,
    audit_status  VARCHAR(20)  NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING/APPROVED/REJECTED',
    license_image_object VARCHAR(512) NULL COMMENT 'MinIO 驾驶证 object key',
    id_card_image_object VARCHAR(512) NULL COMMENT 'MinIO 身份证 object key',
    balance       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '司机';

INSERT INTO t_driver (id, username, password, real_name, audit_status, balance) VALUES
    (1, 'driver1', '123456', '张师傅', 'APPROVED', 0.00),
    (2, 'driver2', '123456', '李师傅', 'APPROVED', 0.00),
    (3, 'driver3', '123456', '王师傅', 'PENDING', 0.00)
ON DUPLICATE KEY UPDATE username=username;

CREATE TABLE IF NOT EXISTS undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(100) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'Seata AT 模式回滚日志';

USE carpool_trip_db;

CREATE TABLE IF NOT EXISTS t_trip (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    passenger_id        BIGINT         NOT NULL,
    trip_mode           VARCHAR(16)    NOT NULL DEFAULT 'SOLO' COMMENT 'SOLO/CARPOOL',
    pool_id             BIGINT         NULL COMMENT '拼车队列 ID',
    driver_id           BIGINT         NULL,
    start_point         VARCHAR(256)   NOT NULL,
    end_point           VARCHAR(256)   NOT NULL,
    start_location      VARCHAR(64)    NULL,
    end_location        VARCHAR(64)    NULL,
    status              VARCHAR(32)    NOT NULL DEFAULT 'CREATED',
    estimated_amount    DECIMAL(12,2)  NOT NULL,
    final_amount        DECIMAL(12,2)  NULL,
    distance_meters     INT            NOT NULL DEFAULT 0,
    duration_seconds    INT            NOT NULL DEFAULT 0,
    passenger_rating    TINYINT        NULL COMMENT '1-5',
    created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_passenger (passenger_id),
    INDEX idx_driver (driver_id),
    INDEX idx_status (status)
) COMMENT '行程';

CREATE TABLE IF NOT EXISTS t_carpool_pool (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    status         VARCHAR(32)  NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/DISPATCHING/COMPLETED/CANCELLED',
    end_point_key  VARCHAR(256) NOT NULL COMMENT '终点匹配键',
    end_point      VARCHAR(256) NOT NULL,
    max_seats      INT          NOT NULL DEFAULT 4,
    current_seats  INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_end (status, end_point_key)
) COMMENT '拼车队列';

CREATE TABLE IF NOT EXISTS t_pricing_rule (
    id           BIGINT PRIMARY KEY,
    base_fare    DECIMAL(12,2) NOT NULL DEFAULT 5.00 COMMENT '起步价（元）',
    per_km_rate  DECIMAL(12,2) NOT NULL DEFAULT 1.50 COMMENT '每公里单价（元）',
    min_fare     DECIMAL(12,2) NOT NULL DEFAULT 10.00 COMMENT '最低消费（元）',
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '计价规则（P4 管理员可改）';

INSERT INTO t_pricing_rule (id, base_fare, per_km_rate, min_fare) VALUES
    (1, 5.00, 1.50, 10.00)
ON DUPLICATE KEY UPDATE base_fare=base_fare;

CREATE TABLE IF NOT EXISTS t_trip_event (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id     BIGINT       NOT NULL,
    from_status VARCHAR(32)  NULL,
    to_status   VARCHAR(32)  NOT NULL,
    operator    VARCHAR(64)  NOT NULL COMMENT 'PASSENGER/DRIVER/SYSTEM',
    remark      VARCHAR(256) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trip (trip_id)
) COMMENT '行程状态流水';

CREATE TABLE IF NOT EXISTS undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(100) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'Seata AT 模式回滚日志';
