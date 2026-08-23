-- ============================================================================
-- 더미데이터: 데모 유저 (카카오 로그인으로 생성된 "첫 번째 유저"에 바인딩)
--   프로필 + 온보딩(증상·트리거·알레르기) + 식사/음식 + 증상 + 스트릭
--
-- 전제:
--   1) Hibernate(ddl-auto)로 테이블 생성 완료
--   2) 카테고리·음식·알레르겐·트리거 마스터가 이미 시딩됨
--   3) 카카오 로그인을 1회 해서 users에 유저가 1명 이상 존재
-- 실행: psql "$DB_URL" -f src/main/resources/db/dummy/dummy_demo_user.sql
--
-- 안전장치:
--   - 시드 음식은 이름을 가정하지 않고 food_id 순서로 5개 선택 → 어떤 시드에도 동작
--   - 프로필/온보딩/스트릭은 ON CONFLICT로 재실행 안전
--   - 식사·증상은 meal_records 존재 여부로 가드 → 재실행해도 중복 안 됨
--   - enum 저장형식: grade/judged_grade/symptom_state/symptom_type/disease_type 는 "이름"
--     (RECOMMEND·CAUTION·RISK / COMFORTABLE·NORMAL·UNCOMFORTABLE / GERD ...),
--     user_symptoms.symptom_code 는 소문자 "코드"(heartburn_reflux ...)
-- ============================================================================

DO $$
DECLARE
    v_user_id bigint;
    v_food1   bigint;
    v_food2   bigint;
    v_food3   bigint;
    v_food4   bigint;
    v_food5   bigint;
    v_meal1   bigint;
    v_meal3   bigint;
    v_meal4   bigint;
    v_sym     bigint;
BEGIN
    -- 카카오 로그인으로 생성된 첫 번째 유저
    SELECT user_id INTO v_user_id FROM users ORDER BY user_id LIMIT 1;
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION '유저가 없습니다. 먼저 카카오 로그인으로 유저를 만든 뒤 실행하세요.';
    END IF;

    -- 시드 음식 5개를 food_id 순서로 선택 (이름 가정 없이 동작)
    SELECT food_id INTO v_food1 FROM foods WHERE source = 'seed' AND deleted_at IS NULL ORDER BY food_id OFFSET 0 LIMIT 1;
    SELECT food_id INTO v_food2 FROM foods WHERE source = 'seed' AND deleted_at IS NULL ORDER BY food_id OFFSET 1 LIMIT 1;
    SELECT food_id INTO v_food3 FROM foods WHERE source = 'seed' AND deleted_at IS NULL ORDER BY food_id OFFSET 2 LIMIT 1;
    SELECT food_id INTO v_food4 FROM foods WHERE source = 'seed' AND deleted_at IS NULL ORDER BY food_id OFFSET 3 LIMIT 1;
    SELECT food_id INTO v_food5 FROM foods WHERE source = 'seed' AND deleted_at IS NULL ORDER BY food_id OFFSET 4 LIMIT 1;
    IF v_food1 IS NULL THEN
        RAISE EXCEPTION '시드 음식이 없습니다. 음식 시딩 후 실행하세요.';
    END IF;
    -- 음식이 5개 미만이면 뒤쪽 변수를 첫 음식으로 폴백 (FK 깨짐 방지)
    v_food2 := COALESCE(v_food2, v_food1);
    v_food3 := COALESCE(v_food3, v_food1);
    v_food4 := COALESCE(v_food4, v_food1);
    v_food5 := COALESCE(v_food5, v_food1);

    -- 1) 온보딩 프로필 (row 존재 = 온보딩 완료) -----------------------------
    INSERT INTO user_profiles (user_id, disease_type, custom_trigger_text, onboarded_at, external_id, created_at, modified_at)
    VALUES (v_user_id, 'GERD', '늦은 야식, 과식하면 속이 쓰려요',
            (now() - interval '30 days')::timestamp, gen_random_uuid(), now(), now())
    ON CONFLICT (user_id) DO NOTHING;

    -- 2) 온보딩 증상 (symptom_code = SymptomCode.code) ----------------------
    INSERT INTO user_symptoms (user_id, symptom_code, external_id, created_at, modified_at)
    VALUES
        (v_user_id, 'heartburn_reflux', gen_random_uuid(), now(), now()),
        (v_user_id, 'post_meal_cough',  gen_random_uuid(), now(), now())
    ON CONFLICT DO NOTHING;

    -- 3) 온보딩 트리거 (code로 trigger_labels 조인) -------------------------
    INSERT INTO user_triggers (user_id, trigger_label_id, external_id, created_at, modified_at)
    SELECT v_user_id, t.trigger_label_id, gen_random_uuid(), now(), now()
    FROM trigger_labels t
    WHERE t.code IN ('caffeine', 'spicy', 'fried_fatty')
    ON CONFLICT DO NOTHING;

    -- 4) 온보딩 알레르기 (code로 allergens 조인) ----------------------------
    INSERT INTO user_allergens (user_id, allergen_id, external_id, created_at, modified_at)
    SELECT v_user_id, a.allergen_id, gen_random_uuid(), now(), now()
    FROM allergens a
    WHERE a.code IN ('milk')
    ON CONFLICT DO NOTHING;

    -- 5)~7) 식사·증상: meal_records가 없을 때만 (재실행 중복 방지) ----
    IF NOT EXISTS (SELECT 1 FROM meal_records WHERE user_id = v_user_id) THEN

        -- 끼니1: 이틀 전 아침 — 편안(RECOMMEND)
        INSERT INTO meal_records (user_id, eaten_at, grade, external_id, created_at, modified_at)
        VALUES (v_user_id, (now() - interval '2 days' - interval '4 hours')::timestamp,
                'RECOMMEND', gen_random_uuid(), now(), now())
        RETURNING id INTO v_meal1;
        INSERT INTO meal_foods (user_id, food_id, meal_record_id, eaten_at, judged_grade, external_id, created_at, modified_at)
        VALUES
            (v_user_id, v_food1, v_meal1, (now() - interval '2 days' - interval '4 hours')::timestamp, 'RECOMMEND', gen_random_uuid(), now(), now()),
            (v_user_id, v_food2, v_meal1, (now() - interval '2 days' - interval '4 hours')::timestamp, 'RECOMMEND', gen_random_uuid(), now(), now());

        -- 끼니2: 어제 점심 — 주의(CAUTION)
        INSERT INTO meal_records (user_id, eaten_at, grade, external_id, created_at, modified_at)
        VALUES (v_user_id, (now() - interval '1 day')::timestamp,
                'CAUTION', gen_random_uuid(), now(), now());

        -- 끼니3: 어제 저녁 — 위험(RISK, 여러 음식 중 최저 등급)
        INSERT INTO meal_records (user_id, eaten_at, grade, external_id, created_at, modified_at)
        VALUES (v_user_id, (now() - interval '1 day' + interval '7 hours')::timestamp,
                'RISK', gen_random_uuid(), now(), now())
        RETURNING id INTO v_meal3;
        INSERT INTO meal_foods (user_id, food_id, meal_record_id, eaten_at, judged_grade, external_id, created_at, modified_at)
        VALUES
            (v_user_id, v_food4, v_meal3, (now() - interval '1 day' + interval '7 hours')::timestamp, 'CAUTION', gen_random_uuid(), now(), now()),
            (v_user_id, v_food5, v_meal3, (now() - interval '1 day' + interval '7 hours')::timestamp, 'RISK',    gen_random_uuid(), now(), now());

        -- 끼니4: 오늘 점심 — 주의(CAUTION)
        INSERT INTO meal_records (user_id, eaten_at, grade, external_id, created_at, modified_at)
        VALUES (v_user_id, (now() - interval '2 hours')::timestamp,
                'CAUTION', gen_random_uuid(), now(), now())
        RETURNING id INTO v_meal4;
        INSERT INTO meal_foods (user_id, food_id, meal_record_id, eaten_at, judged_grade, external_id, created_at, modified_at)
        VALUES (v_user_id, v_food3, v_meal4, (now() - interval '2 hours')::timestamp, 'CAUTION', gen_random_uuid(), now(), now());

        -- 증상1: 이틀 전 아침 식사 후 — 편안
        INSERT INTO symptom_records (user_id, symptom_state, occurred_at, meal_record_id, memo, is_analysis_dirty, analysis_version, external_id, created_at, modified_at)
        VALUES (v_user_id, 'COMFORTABLE', (now() - interval '2 days' - interval '3 hours')::timestamp,
                v_meal1, '아침은 속이 편했어요', true, 0, gen_random_uuid(), now(), now());

        -- 증상2: 어제 저녁 식사 후 — 불편 (증상 유형 2개)
        INSERT INTO symptom_records (user_id, symptom_state, occurred_at, meal_record_id, memo, is_analysis_dirty, analysis_version, external_id, created_at, modified_at)
        VALUES (v_user_id, 'UNCOMFORTABLE', (now() - interval '1 day' + interval '9 hours')::timestamp,
                v_meal3, '야식 후 속쓰림이 심했어요', true, 0, gen_random_uuid(), now(), now())
        RETURNING symptom_id INTO v_sym;
        INSERT INTO symptom_types (symptom_id, symptom_type) VALUES
            (v_sym, 'ACID_REFLUX'),
            (v_sym, 'CHEST_TIGHTNESS');

        -- 증상3: 오늘 — 보통 (식사 미연결)
        INSERT INTO symptom_records (user_id, symptom_state, occurred_at, meal_record_id, memo, is_analysis_dirty, analysis_version, external_id, created_at, modified_at)
        VALUES (v_user_id, 'NORMAL', (now() - interval '1 hour')::timestamp,
                NULL, NULL, true, 0, gen_random_uuid(), now(), now());

    END IF;

    -- 8) 스트릭 (BaseTimeEntity — external_id 없음, version NOT NULL) --------
    INSERT INTO user_streaks (user_id, current_streak, last_comfortable_date, version, created_at, modified_at)
    VALUES (v_user_id, 1, (current_date - 2), 0, now(), now())
    ON CONFLICT (user_id) DO NOTHING;

    RAISE NOTICE '데모 유저 데이터 주입 완료: user_id=%', v_user_id;
END $$;
