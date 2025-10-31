CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    role          VARCHAR(30)  NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);
CREATE TABLE user_settings (
    user_id            UUID PRIMARY KEY REFERENCES users(id),
    locale             VARCHAR(20)  DEFAULT 'sv-SE',
    timezone           VARCHAR(100) DEFAULT 'Europe/Stockholm',
    marketing_opt_in   BOOLEAN      DEFAULT FALSE,
    two_factor_enabled BOOLEAN      DEFAULT FALSE
);
CREATE TABLE activity_log (
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users(id),
    event      VARCHAR(100) NOT NULL,
    meta       TEXT,
    created_at TIMESTAMP    NOT NULL
);
CREATE INDEX idx_activity_user ON activity_log(user_id);
