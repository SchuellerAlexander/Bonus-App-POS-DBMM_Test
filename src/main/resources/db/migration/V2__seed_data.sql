INSERT INTO restaurant (name, code, contact_email, contact_phone, default_currency, timezone, created_at, updated_at)
VALUES ('Demo Restaurant', 'DEMO', 'kontakt@demo.local', '+431234567', 'EUR', 'Europe/Vienna', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO branch (restaurant_id, branch_code, name, address_line, city, country, postal_code)
VALUES ((SELECT id FROM restaurant WHERE code = 'DEMO'), 'HQ', 'Hauptfiliale', 'Hauptplatz 1', 'Leoben', 'AT', '8700');

INSERT INTO customer (external_id, first_name, last_name, email, phone_number, status, created_at, updated_at)
VALUES ('CUST-001', 'Max', 'Muster', 'max.muster@example.com', '+436641234567', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO loyalty_account (account_number, customer_id, restaurant_id, status, current_points, created_at, updated_at)
VALUES ('ACCT-0001',
        (SELECT id FROM customer WHERE external_id = 'CUST-001'),
        (SELECT id FROM restaurant WHERE code = 'DEMO'),
        'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO point_rule (restaurant_id, name, description, rule_type, multiplier, amount_threshold, base_points, valid_from, valid_until, active)
VALUES ((SELECT id FROM restaurant WHERE code = 'DEMO'), 'Standard Multiplikator', '1 Punkt pro Euro', 'MULTIPLIER', 1.00, 1.00, 0, CURRENT_DATE, NULL, TRUE);

INSERT INTO reward (restaurant_id, reward_code, name, description, reward_type, cost_points, valid_from, valid_until, active)
VALUES ((SELECT id FROM restaurant WHERE code = 'DEMO'), 'WELCOME-DRINK', 'Welcome Drink', 'Kostenloses Getränk', 'PRODUCT', 50, CURRENT_DATE, NULL, TRUE);
