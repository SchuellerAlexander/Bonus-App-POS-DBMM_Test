ALTER TABLE customer ADD COLUMN username VARCHAR(60);
ALTER TABLE customer ADD COLUMN password VARCHAR(255);

UPDATE customer
SET username = 'max',
    password = 'pass'
WHERE external_id = 'CUST-001';

ALTER TABLE customer ALTER COLUMN username SET NOT NULL;
ALTER TABLE customer ALTER COLUMN password SET NOT NULL;

ALTER TABLE customer ADD CONSTRAINT uk_customer_username UNIQUE (username);
