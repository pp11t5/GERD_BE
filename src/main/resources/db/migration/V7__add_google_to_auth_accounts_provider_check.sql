ALTER TABLE auth_accounts
    DROP CONSTRAINT IF EXISTS auth_accounts_provider_check;

ALTER TABLE auth_accounts
    ADD CONSTRAINT auth_accounts_provider_check
        CHECK (provider IN ('LOCAL', 'KAKAO', 'APPLE', 'GOOGLE'));
