-- BA summary §2–3: plan is per story (not per user), story lifecycle is
-- DRAFT → PUBLISHED ⇄ HIDDEN → DELETED, and deletion is a status (the row
-- is kept) so quota and data retention can be reasoned about.

-- ARCHIVED had no BA meaning; the closest state is HIDDEN (not public,
-- still owned and editable, still counts toward quota).
UPDATE stories SET status = 'HIDDEN' WHERE status = 'ARCHIVED';

ALTER TABLE stories
    ADD COLUMN plan_type             VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    ADD COLUMN allow_partner_publish BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN owner_transferred_at  TIMESTAMPTZ,
    ADD COLUMN published_at          TIMESTAMPTZ,
    ADD COLUMN expires_at            TIMESTAMPTZ,
    ADD COLUMN deleted_at            TIMESTAMPTZ;

ALTER TABLE stories
    ADD CONSTRAINT ck_stories_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN', 'DELETED')),
    ADD CONSTRAINT ck_stories_plan_type
        CHECK (plan_type IN ('TRIAL', 'PERMANENT', 'COUPLE', 'PRO'));

-- A deleted story releases its slug so it can be claimed again.
DROP INDEX ux_stories_slug;
CREATE UNIQUE INDEX ux_stories_slug ON stories (slug) WHERE status <> 'DELETED';

-- Quota check: COUNT(*) WHERE owner_id = ? AND status <> 'DELETED'.
DROP INDEX ix_stories_owner_id;
CREATE INDEX ix_stories_owner_id_active ON stories (owner_id) WHERE status <> 'DELETED';

-- Trial-expiry job (later step): published TRIAL stories past expires_at.
CREATE INDEX ix_stories_trial_expiry ON stories (expires_at)
    WHERE plan_type = 'TRIAL' AND status = 'PUBLISHED';
