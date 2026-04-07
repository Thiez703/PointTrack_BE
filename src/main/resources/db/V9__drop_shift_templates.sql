-- V9__drop_shift_templates.sql

-- 1. Xóa các foreign key constraints liên quan
ALTER TABLE shifts DROP FOREIGN KEY IF EXISTS FK_shift_template_id;
ALTER TABLE shifts DROP COLUMN IF EXISTS template_id;

ALTER TABLE work_schedules DROP FOREIGN KEY IF EXISTS FK_work_schedule_shift_template_id;
ALTER TABLE work_schedules DROP COLUMN IF EXISTS shift_template_id;

ALTER TABLE service_packages DROP FOREIGN KEY IF EXISTS FK_service_package_template_id;
ALTER TABLE service_packages DROP COLUMN IF EXISTS template_id;

-- 2. Xóa bảng shift_templates
DROP TABLE IF EXISTS shift_templates;
