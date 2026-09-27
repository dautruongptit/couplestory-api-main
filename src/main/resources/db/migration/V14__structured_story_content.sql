-- BA summary v2 §5 (Template Architecture) + §10 (Data Model): the free-form
-- story_sections JSONB bag is replaced by the BA's Shared Content Contract —
-- a fixed set of typed tables every template renders from, so switching
-- template_code can never lose or duplicate data (FR-TEMPLATE-03/04/05).
--
-- Couple / Hero content moves onto `stories` itself (every story has
-- exactly one). Timeline, Love Letter / Final Message and Favorite Moments
-- get their own tables since a story has many of the first and third, and
-- the letter pair share one table distinguished by `type`. Gallery has no
-- table of its own — it *is* the existing `photos` Media Library.

-- ---- Couple / Hero content, now first-class columns on stories ----------

ALTER TABLE stories ADD COLUMN couple_name_1 VARCHAR(100);
ALTER TABLE stories ADD COLUMN couple_name_2 VARCHAR(100);
-- Lossy one-time backfill: `title` was the only place a name lived before.
-- Acceptable pre-launch (no real story data yet — see docs/architecture.md).
UPDATE stories SET couple_name_1 = title, couple_name_2 = '';
ALTER TABLE stories ALTER COLUMN couple_name_1 SET NOT NULL;
ALTER TABLE stories ALTER COLUMN couple_name_2 SET NOT NULL;

ALTER TABLE stories ADD COLUMN start_date DATE;
ALTER TABLE stories ADD COLUMN cover_photo_id UUID REFERENCES photos (id) ON DELETE SET NULL;

-- `template` was a free-form, unvalidated string the frontend invented
-- (e.g. "romantic-candy-pop"). `template_code` replaces it with a backend
-- owned catalog (TemplateCatalog) — Phase 1 ships exactly two codes; a
-- later phase widens the CHECK when it adds more (same pattern as
-- ck_stories_plan_type).
ALTER TABLE stories DROP COLUMN template;
ALTER TABLE stories ADD COLUMN template_code VARCHAR(50) NOT NULL DEFAULT 'minimal-couple';
ALTER TABLE stories ADD CONSTRAINT ck_stories_template_code
    CHECK (template_code IN ('minimal-couple', 'eternal-love'));

-- Presentation-only config for the current template (e.g. a layout toggle).
-- Reset to '{}' whenever template_code changes — see StoryService.
ALTER TABLE stories ADD COLUMN template_config JSONB NOT NULL DEFAULT '{}'::jsonb;

-- ---- Timeline -------------------------------------------------------------

CREATE TABLE story_events (
    id          UUID PRIMARY KEY,
    story_id    UUID NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    event_date  DATE NOT NULL,
    description TEXT,
    photo_id    UUID REFERENCES photos (id) ON DELETE SET NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_story_events_story_id ON story_events (story_id, sort_order);

-- ---- Love Letter + Final Message (BA: same table, distinguished by type) --

CREATE TABLE story_messages (
    id         UUID PRIMARY KEY,
    story_id   UUID NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_story_messages_type CHECK (type IN ('LOVE_LETTER', 'FINAL_MESSAGE'))
);

-- At most one Love Letter and one Final Message per story.
CREATE UNIQUE INDEX ux_story_messages_story_type ON story_messages (story_id, type);

-- ---- Favorite Moments -------------------------------------------------

CREATE TABLE favorite_moments (
    id          UUID PRIMARY KEY,
    story_id    UUID NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    photo_id    UUID REFERENCES photos (id) ON DELETE SET NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_favorite_moments_story_id ON favorite_moments (story_id, sort_order);

-- ---- story_sections is superseded by the tables/columns above ------------

DROP TABLE story_sections;
