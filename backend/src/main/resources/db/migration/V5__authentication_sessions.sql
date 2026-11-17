CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX auth_sessions_user_idx ON auth_sessions(user_id);
CREATE TABLE refresh_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES auth_sessions(id) ON DELETE CASCADE,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX refresh_tokens_session_idx ON refresh_tokens(session_id);
CREATE TABLE auth_rate_limits (
    bucket_key VARCHAR(64) PRIMARY KEY,
    hits INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
