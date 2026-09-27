CREATE TABLE media_assets (
    id            UUID PRIMARY KEY,
    owner_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    story_id      UUID NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    file_name     VARCHAR(255) NOT NULL,
    storage_key   VARCHAR(500),
    content_type  VARCHAR(100),
    file_size     BIGINT,
    status        VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_media_assets_owner_id ON media_assets (owner_id);
CREATE INDEX ix_media_assets_story_id ON media_assets (story_id);
