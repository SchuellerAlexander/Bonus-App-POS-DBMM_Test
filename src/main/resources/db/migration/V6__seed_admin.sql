INSERT INTO customer (first_name, last_name, email, username, password, status, role, created_at, updated_at)
SELECT 'Admin', 'Admin', 'admin@bonus.local', 'admin',
       '$2b$12$3NJ6U./McPNbl25cZWureO7xkfOM0d3AxxTXP4/3uPShNSAeie.na',
       'ACTIVE', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM customer WHERE LOWER(username) = 'admin'
);
