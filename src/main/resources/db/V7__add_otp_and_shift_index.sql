-- FIX: Add OTP fields to users table
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS otp_code VARCHAR(6) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS otp_expired_at DATETIME(3) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS otp_verified TINYINT(1) DEFAULT NULL;

-- FIX: Add shift performance index
ALTER TABLE shifts
  ADD INDEX IF NOT EXISTS idx_shift_status (status);
