CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    keycloak_subject_id VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(320) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_user_profiles_keycloak_subject_id
    ON user_profiles (keycloak_subject_id);

CREATE UNIQUE INDEX uq_user_profiles_email_lower
    ON user_profiles (LOWER(email));
