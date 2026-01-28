ALTER TABLE redemption ADD COLUMN redemption_code VARCHAR(12);

UPDATE redemption
SET redemption_code = CONCAT('R', LPAD(id, 7, '0'));

ALTER TABLE redemption ALTER COLUMN redemption_code SET NOT NULL;
ALTER TABLE redemption ADD CONSTRAINT uk_redemption_code UNIQUE (redemption_code);
