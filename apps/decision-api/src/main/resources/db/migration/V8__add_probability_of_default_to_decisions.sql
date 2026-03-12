-- Add probability_of_default column to decisions table
ALTER TABLE decisions ADD COLUMN probability_of_default DECIMAL(10, 6);
