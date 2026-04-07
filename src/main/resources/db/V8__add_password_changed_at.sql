-- ============================================================
-- V8: Thêm cột password_changed_at vào bảng users
-- Để hỗ trợ invalidate JWT cũ sau khi đổi mật khẩu
-- ============================================================

ALTER TABLE users
    ADD COLUMN password_changed_at DATETIME(3) DEFAULT NULL AFTER password_hash;
