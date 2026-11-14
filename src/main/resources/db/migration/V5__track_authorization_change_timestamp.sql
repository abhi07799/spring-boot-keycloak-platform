ALTER TABLE user_profiles
    ADD COLUMN authorization_updated_at TIMESTAMPTZ NOT NULL DEFAULT TIMESTAMPTZ 'epoch';

