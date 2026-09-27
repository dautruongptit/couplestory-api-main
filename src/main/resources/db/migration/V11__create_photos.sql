-- BA summary §5: Media Library. Only metadata lives here; the files are in
-- photo storage (local volume for MVP, R2/S3 later), already resized and
-- re-encoded to WebP.
--
-- Generic owner (owner_type/owner_id) instead of a story FK so the same
-- table can serve other owners later; consequently there is no FK on
-- owner_id and the service layer enforces it.
CREATE TABLE photos (
    id                 UUID PRIMARY KEY,
    owner_type         VARCHAR(30) NOT NULL,
    owner_id           UUID NOT NULL,
    -- storage keys, e.g. photos/STORY/{owner_id}/{uuid}.webp — URLs are
    -- derived from them so the storage backend can change without a migration
    storage_key        VARCHAR(500) NOT NULL,
    thumbnail_key      VARCHAR(500) NOT NULL,
    filename_original  VARCHAR(255) NOT NULL,
    -- UUID-based name of the stored file; never derived from user input
    filename_stored    VARCHAR(100) NOT NULL,
    mime_type          VARCHAR(50) NOT NULL,
    size_bytes         BIGINT NOT NULL,        -- stored (processed) file
    original_size_bytes BIGINT NOT NULL,       -- as uploaded
    width              INTEGER NOT NULL,
    height             INTEGER NOT NULL,
    sort_order         INTEGER NOT NULL DEFAULT 0,
    uploaded_by        UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_photos_owner_type CHECK (owner_type IN ('STORY'))
);

CREATE UNIQUE INDEX ux_photos_storage_key ON photos (storage_key);
CREATE INDEX ix_photos_owner ON photos (owner_type, owner_id, sort_order);

-- media_assets was an unused Phase 3 placeholder (no upload code ever wrote
-- to it); photos replaces it.
DROP TABLE media_assets;
