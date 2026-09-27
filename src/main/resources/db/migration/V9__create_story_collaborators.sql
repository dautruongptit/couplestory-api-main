-- BA summary §3 (website_collaborators): one partner per story, invited by
-- email, gated on COUPLE/PRO plans. Named story_collaborators to match the
-- existing `stories` table.
CREATE TABLE story_collaborators (
    id                 UUID PRIMARY KEY,
    story_id           UUID NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    -- null until the invite is accepted
    user_id            UUID REFERENCES users (id) ON DELETE CASCADE,
    -- invitee address, lowercase; accept requires the logged-in user's email to match
    email              VARCHAR(255) NOT NULL,
    role               VARCHAR(20) NOT NULL DEFAULT 'PARTNER',
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- SHA-256 of the invite token; the raw token is never stored
    invite_token_hash  VARCHAR(64) NOT NULL,
    invited_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at         TIMESTAMPTZ NOT NULL,
    accepted_at        TIMESTAMPTZ,
    removed_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_story_collaborators_role CHECK (role IN ('PARTNER')),
    CONSTRAINT ck_story_collaborators_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REMOVED')),
    CONSTRAINT ck_story_collaborators_accepted_has_user CHECK (status <> 'ACCEPTED' OR user_id IS NOT NULL)
);

CREATE UNIQUE INDEX ux_story_collaborators_token ON story_collaborators (invite_token_hash);

-- At most one live (pending or accepted) partner per story.
CREATE UNIQUE INDEX ux_story_collaborators_one_live_per_story
    ON story_collaborators (story_id) WHERE status IN ('PENDING', 'ACCEPTED');

-- "Stories I'm a partner on" lookups.
CREATE INDEX ix_story_collaborators_user_accepted
    ON story_collaborators (user_id) WHERE status = 'ACCEPTED';
