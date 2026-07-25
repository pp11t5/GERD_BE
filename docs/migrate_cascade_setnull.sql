-- 유저 하드 삭제 시 콘텐츠 데이터(식사기록·증상기록·도감)를 삭제하지 않고 user_id=NULL로 보존
-- 제약조건명을 동적으로 조회해 적용 — DB 환경마다 이름이 달라도 동작함

DO $$
DECLARE
    v_constraint TEXT;
BEGIN

    -- ── meal_records: CASCADE → SET NULL ──────────────────────────
    SELECT tc.constraint_name INTO v_constraint
    FROM information_schema.table_constraints tc
    JOIN information_schema.referential_constraints rc ON tc.constraint_name = rc.constraint_name
    JOIN information_schema.constraint_column_usage ccu ON rc.unique_constraint_name = ccu.constraint_name
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'meal_records'
      AND kcu.column_name = 'user_id'
      AND ccu.table_name = 'users';

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE meal_records DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
    ALTER TABLE meal_records ALTER COLUMN user_id DROP NOT NULL;
    ALTER TABLE meal_records ADD CONSTRAINT fk_meal_records_user_id
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL;

    -- ── meal_foods: CASCADE → SET NULL ────────────────────────────
    SELECT tc.constraint_name INTO v_constraint
    FROM information_schema.table_constraints tc
    JOIN information_schema.referential_constraints rc ON tc.constraint_name = rc.constraint_name
    JOIN information_schema.constraint_column_usage ccu ON rc.unique_constraint_name = ccu.constraint_name
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'meal_foods'
      AND kcu.column_name = 'user_id'
      AND ccu.table_name = 'users';

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE meal_foods DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
    ALTER TABLE meal_foods ALTER COLUMN user_id DROP NOT NULL;
    ALTER TABLE meal_foods ADD CONSTRAINT fk_meal_foods_user_id
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL;

    -- ── symptom_records: CASCADE → SET NULL ───────────────────────
    SELECT tc.constraint_name INTO v_constraint
    FROM information_schema.table_constraints tc
    JOIN information_schema.referential_constraints rc ON tc.constraint_name = rc.constraint_name
    JOIN information_schema.constraint_column_usage ccu ON rc.unique_constraint_name = ccu.constraint_name
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'symptom_records'
      AND kcu.column_name = 'user_id'
      AND ccu.table_name = 'users';

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE symptom_records DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
    ALTER TABLE symptom_records ALTER COLUMN user_id DROP NOT NULL;
    ALTER TABLE symptom_records ADD CONSTRAINT fk_symptom_records_user_id
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL;

    -- ── user_food_dictionaries: CASCADE → SET NULL ────────────────
    SELECT tc.constraint_name INTO v_constraint
    FROM information_schema.table_constraints tc
    JOIN information_schema.referential_constraints rc ON tc.constraint_name = rc.constraint_name
    JOIN information_schema.constraint_column_usage ccu ON rc.unique_constraint_name = ccu.constraint_name
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'user_food_dictionaries'
      AND kcu.column_name = 'user_id'
      AND ccu.table_name = 'users';

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE user_food_dictionaries DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
    ALTER TABLE user_food_dictionaries ALTER COLUMN user_id DROP NOT NULL;
    ALTER TABLE user_food_dictionaries ADD CONSTRAINT fk_user_food_dictionaries_user_id
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL;

    -- ── weekly_reports: NO ACTION → CASCADE ──────────────────────
    SELECT tc.constraint_name INTO v_constraint
    FROM information_schema.table_constraints tc
    JOIN information_schema.referential_constraints rc ON tc.constraint_name = rc.constraint_name
    JOIN information_schema.constraint_column_usage ccu ON rc.unique_constraint_name = ccu.constraint_name
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'weekly_reports'
      AND kcu.column_name = 'user_id'
      AND ccu.table_name = 'users';

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE weekly_reports DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
    ALTER TABLE weekly_reports ADD CONSTRAINT fk_weekly_reports_user_id
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

    -- ── user_consents: FK 신규 추가 (없으면 고아 데이터 남음) ─────
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'user_consents'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'user_id'
    ) THEN
        ALTER TABLE user_consents
            ADD CONSTRAINT fk_user_consents_user_id
            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;
    END IF;

END $$;
