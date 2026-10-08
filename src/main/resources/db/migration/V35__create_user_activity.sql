-- Per-user activity timeline ("Hoạt động gần đây"). Business events only: no IP, no user agent, no secrets.
CREATE TABLE user_activity (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(30),
    entity_id   UUID,
    summary     VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_activity_user_created ON user_activity (user_id, created_at DESC);
