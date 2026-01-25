-- 1) Branch: default_branch Spalte anlegen (für Hibernate Entity Mapping)
ALTER TABLE branch
ADD COLUMN default_branch BOOLEAN NOT NULL DEFAULT FALSE;

-- Optional: Pro Restaurant genau eine Branch als default setzen (H2-kompatibel)
-- Setzt für jedes restaurant_id die Branch mit der kleinsten ID auf default_branch = TRUE
UPDATE branch b
SET default_branch = TRUE
WHERE b.id IN (
    SELECT MIN(b2.id)
    FROM branch b2
    GROUP BY b2.restaurant_id
);

-- 2) Customer: role Spalte anlegen (für Admin/User Login)
ALTER TABLE customer
ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Optional: bestehenden admin user auf ADMIN setzen (nur wenn er existiert)
UPDATE customer
SET role = 'ADMIN'
WHERE username = 'admin';
