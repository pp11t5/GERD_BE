-- 식사 기록 운영 DDL 참조 (먹어도돼)
--
-- ddl-auto(update)는 엔티티(MealRecord.kt)로부터 테이블을 생성하지만, 아래 두 가지는 JPA로 표현되지 않아
-- 운영 PostgreSQL 하드닝 시 수동 적용한다:
--   1) partial index (WHERE deleted_at IS NULL) — JPA @Index는 부분 인덱스를 만들지 못해 전체 인덱스로 생성된다.
--   2) food FK — meal은 음식 soft-delete 후에도 기록을 보존해야 해(D5) 앱 레벨 plain id 참조다.
--      무결성을 강제하려면 ON DELETE RESTRICT FK를 별도로 건다(소프트 삭제만 쓰면 생략 가능).
--
-- 시각 컬럼은 앱이 KST LocalDateTime으로 매핑한다(프로젝트 전역 컨벤션, JpaAuditingConfig). ADR-0017 참조.

-- 운영에서 ddl-auto 생성 인덱스를 부분 인덱스로 교체할 경우:
DROP INDEX IF EXISTS meal_records_user_eaten_idx;
DROP INDEX IF EXISTS meal_records_group_idx;

-- 타임라인 날짜별 조회 (user_id + eaten_at 범위 스캔; 끼니 그룹핑은 앱에서)
CREATE INDEX meal_records_user_eaten_idx ON meal_records (user_id, eaten_at)
  WHERE deleted_at IS NULL;

-- "같이 먹은 음식" 추가 시 끼니 키 존재·소유 검증
CREATE INDEX meal_records_group_idx ON meal_records (meal_group_id)
  WHERE deleted_at IS NULL;

-- (선택) food 무결성 FK — soft-delete만 쓰는 현 정책에선 생략 가능
-- ALTER TABLE meal_records ADD CONSTRAINT fk_meal_records_food
--   FOREIGN KEY (food_id) REFERENCES foods(food_id) ON DELETE RESTRICT;
