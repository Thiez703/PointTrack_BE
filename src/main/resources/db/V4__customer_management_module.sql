-- ============================================================
-- V4 – Customer Management Module
-- Thêm/sửa bảng customers để hỗ trợ module Quản lý Khách hàng
-- ============================================================

-- Thêm các cột mới vào bảng customers (nếu chưa có)

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS phone              VARCHAR(20)   NULL COMMENT 'SĐT chính' AFTER name,
    ADD COLUMN IF NOT EXISTS secondary_phone   VARCHAR(20)   NULL COMMENT 'SĐT phụ' AFTER phone,
    ADD COLUMN IF NOT EXISTS address           TEXT          NULL COMMENT 'Địa chỉ đầy đủ (dạng chuỗi)' AFTER secondary_phone,
    ADD COLUMN IF NOT EXISTS special_notes     TEXT          NULL COMMENT 'Ghi chú đặc biệt' AFTER longitude,
    ADD COLUMN IF NOT EXISTS preferred_time_note VARCHAR(255) NULL COMMENT 'Khung giờ ưa thích' AFTER special_notes,
    ADD COLUMN IF NOT EXISTS source            ENUM('ZALO','FACEBOOK','REFERRAL','HOTLINE','OTHER')
                                               NOT NULL DEFAULT 'OTHER' COMMENT 'Nguồn KH' AFTER preferred_time_note,
    ADD COLUMN IF NOT EXISTS status            ENUM('ACTIVE','INACTIVE','SUSPENDED')
                                               NOT NULL DEFAULT 'ACTIVE' COMMENT 'Trạng thái KH' AFTER source;

-- Thêm index mới (bỏ qua lỗi nếu đã tồn tại)

CREATE INDEX IF NOT EXISTS idx_customer_phone  ON customers (phone);
CREATE INDEX IF NOT EXISTS idx_customer_status ON customers (status);

-- Migrate data cũ: đồng bộ is_active → status
UPDATE customers
SET status = CASE
    WHEN is_active = false OR is_active IS NULL THEN 'INACTIVE'
    ELSE 'ACTIVE'
END
WHERE status = 'ACTIVE'
  AND is_active IS NOT NULL;

-- Migrate data cũ: ghép street/ward/district/city → address (chỉ khi address còn NULL)
UPDATE customers
SET address = TRIM(BOTH ', ' FROM CONCAT_WS(', ',
    NULLIF(TRIM(street),   ''),
    NULLIF(TRIM(ward),     ''),
    NULLIF(TRIM(district), ''),
    NULLIF(TRIM(city),     '')
))
WHERE address IS NULL
  AND (street IS NOT NULL OR ward IS NOT NULL OR district IS NOT NULL OR city IS NOT NULL);

-- Migrate data cũ: phone_number → phone (chỉ khi phone còn NULL)
UPDATE customers
SET phone = phone_number
WHERE phone IS NULL
  AND phone_number IS NOT NULL;
