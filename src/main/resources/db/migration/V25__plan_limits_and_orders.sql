-- Plan limits from BA V1.3 §4.2 and manual-payment orders (BA §13).

ALTER TABLE plans
    ADD COLUMN website_duration_days  INT,                              -- NULL = permanent
    ADD COLUMN max_total_stories      INT,                              -- lifetime cap, NULL = unlimited
    ADD COLUMN max_music_tracks       INT     NOT NULL DEFAULT 1,
    ADD COLUMN max_photos_per_event   INT     NOT NULL DEFAULT 5,
    ADD COLUMN allow_password         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN show_watermark         BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE plans SET website_duration_days = 30,  max_total_stories = 10,   max_music_tracks = 1,  max_photos_per_event = 1, allow_password = FALSE, show_watermark = TRUE  WHERE code = 'FREE';
UPDATE plans SET website_duration_days = 180, max_total_stories = 50,   max_music_tracks = 5,  max_photos_per_event = 5, allow_password = TRUE,  show_watermark = FALSE WHERE code = 'PLUS';
UPDATE plans SET website_duration_days = 365, max_total_stories = NULL, max_music_tracks = 10, max_photos_per_event = 5, allow_password = TRUE,  show_watermark = FALSE WHERE code = 'COUPLE';
UPDATE plans SET website_duration_days = NULL, max_total_stories = NULL, max_music_tracks = 20, max_photos_per_event = 5, allow_password = TRUE,  show_watermark = FALSE WHERE code = 'PREMIUM';

-- Manual payment: the user transfers money with a transfer code, an admin confirms.
CREATE TABLE orders (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type          VARCHAR(20)  NOT NULL,
    plan_code     VARCHAR(20),
    story_id      UUID         REFERENCES stories (id) ON DELETE SET NULL,
    renew_term    VARCHAR(10),
    amount        BIGINT       NOT NULL,
    transfer_code VARCHAR(20)  NOT NULL UNIQUE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    paid_at       TIMESTAMPTZ,
    confirmed_by  UUID         REFERENCES users (id) ON DELETE SET NULL,

    CONSTRAINT ck_orders_type   CHECK (type IN ('UPGRADE', 'RENEW')),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT ck_orders_term   CHECK (renew_term IS NULL OR renew_term IN ('3M', '1Y')),
    CONSTRAINT ck_orders_shape  CHECK (
        (type = 'UPGRADE' AND plan_code IS NOT NULL AND renew_term IS NULL)
        OR (type = 'RENEW' AND story_id IS NOT NULL AND renew_term IS NOT NULL))
);

CREATE INDEX ix_orders_user   ON orders (user_id, created_at DESC);
CREATE INDEX ix_orders_status ON orders (status, created_at);

-- Expiry job: published stories past expires_at.
CREATE INDEX ix_stories_published_expiry ON stories (expires_at)
    WHERE status = 'PUBLISHED' AND expires_at IS NOT NULL;
