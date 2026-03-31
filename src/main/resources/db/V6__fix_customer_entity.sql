-- FIX: Remove deprecated columns from customers table
ALTER TABLE customers
  DROP COLUMN IF EXISTS phone_number,
  DROP COLUMN IF EXISTS note,
  DROP COLUMN IF EXISTS is_active,
  DROP COLUMN IF EXISTS email,
  DROP COLUMN IF EXISTS street,
  DROP COLUMN IF EXISTS ward,
  DROP COLUMN IF EXISTS district,
  DROP COLUMN IF EXISTS city;
