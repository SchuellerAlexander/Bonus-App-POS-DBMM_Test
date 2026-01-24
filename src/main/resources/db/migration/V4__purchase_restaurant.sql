ALTER TABLE purchase ADD COLUMN restaurant_id BIGINT;
UPDATE purchase
SET restaurant_id = (
    SELECT branch.restaurant_id
    FROM branch
    WHERE branch.id = purchase.branch_id
);
ALTER TABLE purchase ALTER COLUMN restaurant_id SET NOT NULL;
ALTER TABLE purchase
    ADD CONSTRAINT fk_purchase_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant (id);
CREATE INDEX idx_purchase_restaurant ON purchase (restaurant_id);

ALTER TABLE purchase DROP CONSTRAINT fk_purchase_branch;
DROP INDEX idx_purchase_branch;
ALTER TABLE purchase DROP COLUMN branch_id;
