-- ============================================================
-- V2: Shift Scheduling Module
-- Tables: shift_templates (alter), shifts, service_packages
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. Thêm cột notes vào shift_templates (nếu chưa có)
-- ─────────────────────────────────────────────────────────────
ALTER TABLE shift_templates
    ADD COLUMN IF NOT EXISTS notes TEXT DEFAULT NULL AFTER ot_multiplier;

-- ─────────────────────────────────────────────────────────────
-- 2. service_packages — phải tạo trước vì shifts tham chiếu tới
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS service_packages
(
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    customer_id         BIGINT          NOT NULL,
    employee_id         BIGINT          NOT NULL,
    template_id         BIGINT          DEFAULT NULL,
    total_sessions      INT             NOT NULL,
    completed_sessions  INT             NOT NULL DEFAULT 0,
    recurrence_pattern  JSON            NOT NULL COMMENT '{"days":[1,3,5],"time":"08:00"}',
    notes               TEXT            DEFAULT NULL,
    status              ENUM('ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    -- BaseEntity audit columns
    created_at          DATETIME(6)     DEFAULT NULL,
    updated_at          DATETIME(6)     DEFAULT NULL,
    created_by_user_id  BIGINT          DEFAULT NULL,
    updated_by_user_id  BIGINT          DEFAULT NULL,

    PRIMARY KEY (id),
    INDEX idx_pkg_customer  (customer_id),
    INDEX idx_pkg_employee  (employee_id),
    INDEX idx_pkg_status    (status),

    CONSTRAINT fk_pkg_customer  FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_pkg_employee  FOREIGN KEY (employee_id) REFERENCES users (id),
    CONSTRAINT fk_pkg_template  FOREIGN KEY (template_id) REFERENCES shift_templates (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- 3. shifts
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS shifts
(
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id         BIGINT          NOT NULL,
    customer_id         BIGINT          NOT NULL,
    template_id         BIGINT          DEFAULT NULL,
    package_id          BIGINT          DEFAULT NULL,
    shift_date          DATE            NOT NULL,
    start_time          TIME            NOT NULL,
    end_time            TIME            NOT NULL
        COMMENT 'BR-10: NORMAL/HOLIDAY end>start. OT_EMERGENCY end<start = qua đêm',
    duration_minutes    INT             NOT NULL
        COMMENT 'BR-06: Cố định từ template, không tính từ checkin/checkout',
    shift_type          ENUM('NORMAL','HOLIDAY','OT_EMERGENCY') NOT NULL DEFAULT 'NORMAL',
    ot_multiplier       DECIMAL(3, 1)   NOT NULL DEFAULT 1.0,
    notes               TEXT            DEFAULT NULL,
    status              ENUM('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    -- BaseEntity audit columns
    created_at          DATETIME(6)     DEFAULT NULL,
    updated_at          DATETIME(6)     DEFAULT NULL,
    created_by_user_id  BIGINT          DEFAULT NULL,
    updated_by_user_id  BIGINT          DEFAULT NULL,

    PRIMARY KEY (id),
    INDEX idx_shift_employee_date (employee_id, shift_date)
        COMMENT 'BR-13: tra cứu conflict nhanh theo nhân viên + ngày',
    INDEX idx_shift_package       (package_id),
    INDEX idx_shift_date_status   (shift_date, status),

    CONSTRAINT fk_shift_employee FOREIGN KEY (employee_id) REFERENCES users (id),
    CONSTRAINT fk_shift_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_shift_template FOREIGN KEY (template_id) REFERENCES shift_templates (id),
    CONSTRAINT fk_shift_package  FOREIGN KEY (package_id)  REFERENCES service_packages (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- Notes:
--   BR-06  Salary tính theo duration_minutes cố định, không phải thực tế checkin/out
--   BR-09  Buffer 15 phút giữa 2 ca liên tiếp của cùng nhân viên (service layer)
--   BR-10  NORMAL/HOLIDAY: end_time > start_time. OT_EMERGENCY: end_time < start_time = qua đêm
--   BR-13  Conflict detection: OVERLAP (hard block) | BUFFER (warn/block)
-- ─────────────────────────────────────────────────────────────
