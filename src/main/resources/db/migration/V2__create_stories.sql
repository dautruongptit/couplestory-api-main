CREATE TABLE stories (
    id           UUID PRIMARY KEY,
    owner_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    slug         VARCHAR(150) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    template     VARCHAR(100) NOT NULL DEFAULT 'romanticCandyPop',
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_stories_slug ON stories (slug);
CREATE INDEX ix_stories_owner_id ON stories (owner_id);
