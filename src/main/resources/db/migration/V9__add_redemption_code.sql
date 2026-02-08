ALTER TABLE redemption ADD COLUMN redemption_code VARCHAR(12);

UPDATE redemption
SET redemption_code = SUBSTRING(UPPER(REPLACE(RANDOM_UUID(), '-', '')), 1, 12)
WHERE redemption_code IS NULL;

ALTER TABLE redemption ALTER COLUMN redemption_code SET NOT NULL;

CREATE UNIQUE INDEX uk_redemption_code ON redemption(redemption_code);
