ALTER TABLE auth_accounts
    ADD COLUMN IF NOT EXISTS provider_refresh_token VARCHAR(1000);
