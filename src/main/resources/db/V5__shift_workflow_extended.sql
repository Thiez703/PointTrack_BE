-- ============================================================
-- V5: Mở rộng luồng quản lý ca làm việc (end-to-end workflow)
-- ============================================================

-- 1. Mở rộng ENUM status: thêm DRAFT, PUBLISHED, ASSIGNED, CONFIRMED, MISSED, MISSING_OUT
ALTER TABLE shifts
    MODIFY COLUMN status ENUM(
        'DRAFT',
        'PUBLISHED',
        'ASSIGNED',
        'SCHEDULED',
        'CONFIRMED',
        'IN_PROGRESS',
        'COMPLETED',
        'MISSED',
        'MISSING_OUT',
        'CANCELLED'
    ) NOT NULL DEFAULT 'ASSIGNED';

-- 2. Cho phép employee_id NULL (ca trống - PUBLISHED)
ALTER TABLE shifts
    MODIFY COLUMN employee_id BIGINT NULL;

-- 3. Thêm cột ảnh check-in
ALTER TABLE shifts
    ADD COLUMN check_in_photo VARCHAR(500) NULL
        COMMENT 'URL ảnh hiện trường khi check-in (bắt buộc nếu ngoài geofence)'
        AFTER check_in_distance_meters;

-- 4. Thêm cột thực tế làm việc (phút)
ALTER TABLE shifts
    ADD COLUMN actual_minutes INT NULL
        COMMENT 'Số phút thực tế làm việc = checkOutTime - checkInTime'
        AFTER check_out_distance_meters;

-- 5. Index hỗ trợ query ca trống
ALTER TABLE shifts
    ADD INDEX idx_shift_open_slots (status, shift_date, employee_id);
