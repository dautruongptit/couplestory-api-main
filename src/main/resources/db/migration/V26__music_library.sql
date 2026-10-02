-- Background music: a library managed by admins; every track is usable on every plan.
-- The number of tracks per website is limited by plans.max_music_tracks.

CREATE TABLE music_tracks (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title          VARCHAR(150) NOT NULL,
    artist         VARCHAR(150),
    stored_file    VARCHAR(150) NOT NULL,        -- served from /uploads/<stored_file>
    license_note   VARCHAR(300),                 -- where the licence/permission comes from
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE story_music (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID    NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    track_id    UUID    NOT NULL REFERENCES music_tracks (id) ON DELETE CASCADE,
    sort_order  INT     NOT NULL DEFAULT 0,
    CONSTRAINT ux_story_music UNIQUE (story_id, track_id)
);

CREATE INDEX ix_story_music_story ON story_music (story_id, sort_order);
