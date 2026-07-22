USE carpool_passenger_db;
ALTER TABLE t_passenger ADD COLUMN nickname VARCHAR(64) NULL COMMENT '昵称' AFTER username;
ALTER TABLE t_passenger ADD COLUMN avatar_object VARCHAR(512) NULL COMMENT 'MinIO 头像 object key' AFTER nickname;

USE carpool_driver_db;
ALTER TABLE t_driver ADD COLUMN nickname VARCHAR(64) NULL COMMENT '昵称' AFTER username;
ALTER TABLE t_driver ADD COLUMN avatar_object VARCHAR(512) NULL COMMENT 'MinIO 头像 object key' AFTER nickname;

USE carpool_trip_db;
ALTER TABLE t_trip ADD COLUMN trip_mode VARCHAR(16) NOT NULL DEFAULT 'SOLO' COMMENT 'SOLO/CARPOOL' AFTER passenger_id;
ALTER TABLE t_trip ADD COLUMN pool_id BIGINT NULL COMMENT '拼车队列 ID' AFTER trip_mode;

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
