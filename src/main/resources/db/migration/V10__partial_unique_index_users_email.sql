-- 탈퇴(soft-delete)한 유저의 이메일이 deleted_at과 무관한 전역 unique 제약에 남아있어
-- 같은 이메일로 재가입하거나 다른 provider로 로그인하면 duplicate key 충돌이 발생하던 문제 수정.
-- 활성(deleted_at IS NULL) 유저 사이에서만 유니크하도록 partial unique index로 교체한다.

-- CONCURRENTLY로 먼저 새 인덱스를 만들어 빌드 중 users 쓰기 잠금을 피한다 (spring.flyway.mixed 필요, V1과 동일 패턴)
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS ux_users_email_active ON users (email) WHERE deleted_at IS NULL;

-- DROP CONSTRAINT는 메타데이터 변경이라 짧은 배타적 잠금만 필요 — 새 인덱스가 이미 있으니 이 시점부터 제약 공백 없음
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT con.conname INTO constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
    WHERE rel.relname = 'users'
      AND att.attname = 'email'
      AND con.contype = 'u';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;
