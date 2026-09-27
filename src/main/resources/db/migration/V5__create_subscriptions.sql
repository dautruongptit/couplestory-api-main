CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan        VARCHAR(50) NOT NULL DEFAULT 'FREE',
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ends_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_subscriptions_user_id ON subscriptions (user_id);
