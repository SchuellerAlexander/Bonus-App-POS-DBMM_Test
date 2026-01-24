ALTER TABLE restaurant ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE branch ADD COLUMN default_branch BOOLEAN NOT NULL DEFAULT FALSE;

WITH ranked_branches AS (
    SELECT id,
           restaurant_id,
           ROW_NUMBER() OVER (PARTITION BY restaurant_id ORDER BY id) AS branch_rank
    FROM branch
)
UPDATE branch
SET default_branch = TRUE
FROM ranked_branches
WHERE branch.id = ranked_branches.id
  AND ranked_branches.branch_rank = 1;
