-- 1) Neue Admin-Felder hinzufügen
ALTER TABLE restaurant ADD COLUMN active BOOLEAN DEFAULT TRUE;
ALTER TABLE branch ADD COLUMN active BOOLEAN DEFAULT TRUE;
ALTER TABLE branch ADD COLUMN default_branch BOOLEAN DEFAULT FALSE;

-- 2) Genau eine Default-Branch pro Restaurant setzen
-- (H2-kompatible Variante ohne UPDATE ... FROM)

UPDATE branch
SET default_branch = TRUE
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY restaurant_id ORDER BY id) AS rn
        FROM branch
    ) ranked
    WHERE ranked.rn = 1
);

-- 3) NOT NULL Constraints
ALTER TABLE restaurant ALTER COLUMN active SET NOT NULL;
ALTER TABLE branch ALTER COLUMN active SET NOT NULL;
ALTER TABLE branch ALTER COLUMN default_branch SET NOT NULL;
