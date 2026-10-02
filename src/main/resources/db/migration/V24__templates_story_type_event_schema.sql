-- Template catalog, Story.type and the new Event schema
-- (Event, Type & Template Architecture Rules; BA V1.3).

-- ---- Templates -------------------------------------------------------------

CREATE TABLE templates (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                   VARCHAR(50)  NOT NULL UNIQUE,
    name                   VARCHAR(100) NOT NULL,
    type                   VARCHAR(20)  NOT NULL DEFAULT 'LOVE_STORY',
    package                VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    description            TEXT,
    preview_image          VARCHAR(500),
    recommended_events     INT          NOT NULL DEFAULT 5,
    max_display_events     INT          NOT NULL DEFAULT 6,
    min_events_for_publish INT          NOT NULL DEFAULT 2,
    sort_order             INT          NOT NULL DEFAULT 0,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_templates_type    CHECK (type IN ('LOVE_STORY', 'LOVE_CARD')),
    CONSTRAINT ck_templates_package CHECK (package IN ('FREE', 'PLUS', 'COUPLE', 'PREMIUM')),
    -- Each template shows 4-6 events
    CONSTRAINT ck_templates_events  CHECK (
        max_display_events BETWEEN 4 AND 6
        AND recommended_events BETWEEN 4 AND 6
        AND recommended_events <= max_display_events
        AND min_events_for_publish >= 1
        AND min_events_for_publish <= max_display_events)
);

CREATE INDEX ix_templates_active ON templates (sort_order) WHERE is_active = TRUE;

INSERT INTO templates (code, name, type, package, description, sort_order, is_active) VALUES
    ('minimal-couple',       'Minimal Couple',       'LOVE_STORY', 'FREE',    'Phong cách tối giản, tập trung vào hình ảnh và khoảng trắng.', 1, TRUE),
    ('romantic-anniversary', 'Romantic Anniversary', 'LOVE_STORY', 'FREE',    'Lãng mạn mơ mộng với hiệu ứng trái tim bay.', 2, TRUE),
    ('memory-wall',          'Romantic Memory Wall', 'LOVE_STORY', 'FREE',    'Bức tường kỷ niệm với ảnh polaroid.', 3, TRUE),
    ('autumn-paris',         'Autumn Paris Romance', 'LOVE_STORY', 'PLUS',    'Mùa thu Paris lãng mạn.', 4, TRUE),
    ('sunset-horizon',       'Sunset Horizon',       'LOVE_STORY', 'PLUS',    'Hoàng hôn trên đường chân trời.', 5, TRUE),
    ('sweet-polaroid',       'Sweet Polaroid Story', 'LOVE_STORY', 'PLUS',    'Câu chuyện qua những tấm polaroid.', 6, TRUE),
    ('anniversary-journey',  'Anniversary Journey',  'LOVE_STORY', 'COUPLE',  'Hành trình tình yêu theo từng cột mốc.', 7, TRUE),
    ('royal-wedding',        'Royal Wedding Memoir', 'LOVE_STORY', 'COUPLE',  'Hồi ký đám cưới hoàng gia.', 8, TRUE),
    ('eternal-love',         'Eternal Love',         'LOVE_STORY', 'PREMIUM', 'Sang trọng, lãng mạn như một cuốn tạp chí cưới.', 9, TRUE);

-- Occasion templates: built, but inactive until the WEDDING / BIRTHDAY / LOVE_CARD
-- content models exist (kept hidden in the gallery).
INSERT INTO templates (code, name, type, package, description, sort_order, is_active) VALUES
    ('wedding-cinematic',         'Wedding Cinematic',         'LOVE_CARD', 'PREMIUM', 'Đám cưới điện ảnh nền tối, vàng kim.', 20, FALSE),
    ('wedding-garden-bloom',      'Wedding Garden Bloom',      'LOVE_CARD', 'PREMIUM', 'Đám cưới vườn hoa sáng, xanh sage và hồng đất.', 21, FALSE),
    ('birthday-neon-party',       'Birthday Neon Party',       'LOVE_CARD', 'PREMIUM', 'Sinh nhật neon Y2K nền tối.', 22, FALSE),
    ('birthday-soft-yume',        'Birthday Soft Yume',        'LOVE_CARD', 'FREE',    'Sinh nhật pastel mộng mơ.', 23, FALSE),
    ('confession-typewriter',     'Confession Typewriter',     'LOVE_CARD', 'FREE',    'Tỏ tình bằng lá thư đánh máy.', 24, FALSE),
    ('confession-midnight-bloom', 'Confession Midnight Bloom', 'LOVE_CARD', 'PREMIUM', 'Tỏ tình đêm tím huyền ảo.', 25, FALSE);

-- ---- Story.type ------------------------------------------------------------

ALTER TABLE stories ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'LOVE_STORY';
ALTER TABLE stories ADD CONSTRAINT ck_stories_type CHECK (type IN ('LOVE_STORY', 'LOVE_CARD'));

-- ---- Event schema ----------------------------------------------------------
-- message replaces description (the old column is kept, unused, so nothing is lost);
-- date becomes optional; Event.type is NOT stored (derived from Story.type).

ALTER TABLE story_events ADD COLUMN message VARCHAR(500) NOT NULL DEFAULT '';
UPDATE story_events SET message = LEFT(COALESCE(description, ''), 500);

ALTER TABLE story_events ADD COLUMN location   VARCHAR(150);
ALTER TABLE story_events ADD COLUMN is_visible BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE story_events ALTER COLUMN event_date DROP NOT NULL;

UPDATE story_events SET title = LEFT(title, 100) WHERE char_length(title) > 100;
ALTER TABLE story_events ALTER COLUMN title TYPE VARCHAR(100);
