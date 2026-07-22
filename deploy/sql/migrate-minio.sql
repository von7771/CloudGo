USE carpool_driver_db;

ALTER TABLE t_driver
    ADD COLUMN license_image_object VARCHAR(512) NULL COMMENT 'MinIO 驾驶证 object key' AFTER audit_status;

ALTER TABLE t_driver
    ADD COLUMN id_card_image_object VARCHAR(512) NULL COMMENT 'MinIO 身份证 object key' AFTER license_image_object;
