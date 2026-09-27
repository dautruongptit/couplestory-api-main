-- BA summary §6: in-app notifications (bell icon) alongside email.
CREATE TABLE notifications (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type              VARCHAR(40) NOT NULL,
    title             VARCHAR(200) NOT NULL,
    content           TEXT NOT NULL,
    related_story_id  UUID REFERENCES stories (id) ON DELETE SET NULL,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_notifications_type CHECK (type IN (
        'TRIAL_EXPIRING', 'TRIAL_EXPIRED', 'INVITE_RECEIVED',
        'PARTNER_JOINED', 'PARTNER_REMOVED', 'OWNER_TRANSFERRED'))
);

-- Bell dropdown: newest first per user.
CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC);

-- Badge polling: COUNT(*) WHERE user_id = ? AND NOT is_read.
CREATE INDEX ix_notifications_user_unread ON notifications (user_id) WHERE NOT is_read;

-- BA summary §2 (warning_sent_at): set once the "trial ends in 24h" warning
-- has gone out, so the job never sends it twice.
ALTER TABLE stories ADD COLUMN trial_warning_sent_at TIMESTAMPTZ;
