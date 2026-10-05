-- Apple/Google/Kakao 등 서로 다른 provider가 같은 이메일로 각각 독립된 유저를 가질 수 있도록
-- (email은 유저 식별/병합 키로 쓰지 않음) users.email의 DB unique 제약을 제거한다.
-- 탈퇴(soft-delete)한 유저의 이메일이 deleted_at과 무관하게 이 제약에 남아있어, 같은 이메일로
-- 재가입하거나 다른 provider로 로그인하면 duplicate key 충돌(409)이 발생하던 문제도 함께 해결된다.
-- DROP CONSTRAINT는 메타데이터 변경이라 짧은 배타적 잠금만 필요.

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
