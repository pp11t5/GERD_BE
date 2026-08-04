WITH ranked_dictionaries AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, food_id
               ORDER BY modified_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) AS row_number
    FROM user_food_dictionaries
)
DELETE FROM user_food_dictionaries dictionary
USING ranked_dictionaries ranked
WHERE dictionary.id = ranked.id
  AND ranked.row_number > 1;

ALTER TABLE user_food_dictionaries
    DROP CONSTRAINT IF EXISTS uq_user_food_dict;

ALTER TABLE user_food_dictionaries
    ADD CONSTRAINT uq_user_food_dict UNIQUE (user_id, food_id);
