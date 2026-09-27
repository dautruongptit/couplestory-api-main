CREATE TABLE story_sections (
    id          UUID PRIMARY KEY,
    story_id    UUID NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    type        VARCHAR(50) NOT NULL,
    variant     VARCHAR(50),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    content     JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_story_sections_story_id ON story_sections (story_id);
