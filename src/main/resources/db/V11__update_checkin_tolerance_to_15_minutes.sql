-- Cập nhật dung sai check-in mặc định về ±15 phút (BR-11)

UPDATE system_settings
SET setting_value = '15',
    description = 'Dung sai check-in ±15 phút vẫn tính đúng giờ (BR-11)',
    updated_at = CURRENT_TIMESTAMP
WHERE setting_key = 'GRACE_PERIOD_MINUTES';

INSERT INTO system_settings (setting_key, setting_value, description, updated_at, updated_by_user_id)
SELECT 'GRACE_PERIOD_MINUTES',
       '15',
       'Dung sai check-in ±15 phút vẫn tính đúng giờ (BR-11)',
       CURRENT_TIMESTAMP,
       NULL
WHERE NOT EXISTS (
    SELECT 1 FROM system_settings WHERE setting_key = 'GRACE_PERIOD_MINUTES'
);
