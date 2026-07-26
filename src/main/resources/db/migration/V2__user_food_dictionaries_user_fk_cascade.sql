-- user_food_dictionaries.user_id FK를 CASCADE로 통일
-- 기존에는 ON DELETE SET NULL(환경별로 제약 이름이 다르게 남아있음)이라
-- 회원 하드 삭제 후에도 주인 없는 도감 행이 그대로 남았다.
-- 도감은 유저 개인 데이터이므로 유저 삭제 시 함께 삭제되도록 CASCADE로 재생성한다.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'user_food_dictionaries'::regclass
          AND contype = 'f'
          AND confrelid = 'users'::regclass
    LOOP
        EXECUTE format('ALTER TABLE user_food_dictionaries DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE user_food_dictionaries
    ADD CONSTRAINT fk_user_food_dictionaries_user_id
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;
