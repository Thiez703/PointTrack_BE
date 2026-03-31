-- ============================================================
-- V3: Employee Management Module
-- - Thêm cột area, skills vào bảng users
-- - Mở rộng ENUM status của users để có ON_LEAVE
-- - Tạo bảng salary_level_history
-- ============================================================

-- 1. Thêm cột area và skills vào bảng users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS area   VARCHAR(100)  DEFAULT NULL COMMENT 'Khu vực phụ trách',
    ADD COLUMN IF NOT EXISTS skills JSON          DEFAULT NULL COMMENT 'Danh sách kỹ năng, VD: ["tam_be","ve_sinh"]';

-- 2. Mở rộng ENUM status để hỗ trợ ON_LEAVE
ALTER TABLE users
    MODIFY COLUMN status ENUM('ACTIVE','INACTIVE','ON_LEAVE') NOT NULL DEFAULT 'ACTIVE';

-- 3. Tạo bảng lịch sử thay đổi cấp bậc lương
CREATE TABLE IF NOT EXISTS salary_level_history (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT UNSIGNED  NOT NULL,
    old_level_id   BIGINT UNSIGNED  DEFAULT NULL,
    new_level_id   BIGINT UNSIGNED  NOT NULL,
    effective_date DATE             NOT NULL,
    changed_by     BIGINT UNSIGNED  NOT NULL,
    reason         TEXT             DEFAULT NULL,
    created_at     DATETIME(3)      NOT NULL DEFAULT NOW(3),
    PRIMARY KEY (id),
    INDEX idx_slh_employee  (employee_id),
    INDEX idx_slh_effective (effective_date),
    CONSTRAINT fk_slh_employee   FOREIGN KEY (employee_id)  REFERENCES users(id),
    CONSTRAINT fk_slh_old_level  FOREIGN KEY (old_level_id) REFERENCES salary_levels(id),
    CONSTRAINT fk_slh_new_level  FOREIGN KEY (new_level_id) REFERENCES salary_levels(id),
    CONSTRAINT fk_slh_changed_by FOREIGN KEY (changed_by)   REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lịch sử thay đổi cấp bậc lương của nhân viên';
