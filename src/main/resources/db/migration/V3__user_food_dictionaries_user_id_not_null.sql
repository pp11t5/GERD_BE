-- 도감은 유저 개인 데이터라 user_id가 없는 행은 존재 불가
-- 과거 ON DELETE SET NULL 잔재로 남은 주인 없는 행 정리 후 NOT NULL 강제
DELETE FROM user_food_dictionaries WHERE user_id IS NULL;

ALTER TABLE user_food_dictionaries
    ALTER COLUMN user_id SET NOT NULL;
