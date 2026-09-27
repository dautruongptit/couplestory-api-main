-- Email verification and password reset links, moved from the Next.js app
-- (Prisma VerificationToken) into the API, which is now the only backend.
CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(30) NOT NULL,
    -- SHA-256 of the token in the emailed link; the raw token is never stored
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_auth_tokens_purpose CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD'))
);

CREATE UNIQUE INDEX ux_auth_tokens_hash ON auth_tokens (token_hash);
-- latest token per user/purpose (resend cooldown, invalidating older links)
CREATE INDEX ix_auth_tokens_user_purpose ON auth_tokens (user_id, purpose, created_at DESC);

-- Google sign-in: the stable Google account id ("sub" claim). Linked on
-- first Google login; emails can change, sub never does.
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255);
CREATE UNIQUE INDEX ux_users_google_sub ON users (google_sub) WHERE google_sub IS NOT NULL;
