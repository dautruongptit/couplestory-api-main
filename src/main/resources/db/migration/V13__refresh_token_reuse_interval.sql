-- When a refresh token was revoked because it was rotated (as opposed to
-- logout or theft detection). Lets a token that was rotated moments ago be
-- presented again by concurrent requests without being treated as theft.
ALTER TABLE refresh_tokens ADD COLUMN rotated_at TIMESTAMPTZ;
