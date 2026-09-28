CREATE TABLE plans (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    price       BIGINT       NOT NULL DEFAULT 0,
    description TEXT,
    features    JSONB        NOT NULL DEFAULT '[]',
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN      NOT NULL DEFAULT FALSE,
    max_photos  INT,
    max_stories INT          NOT NULL DEFAULT 1,
    allow_collaborator   BOOLEAN NOT NULL DEFAULT FALSE,
    allow_custom_domain  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_plans_active ON plans (sort_order) WHERE is_active = TRUE;

INSERT INTO plans (code, name, price, description, features, sort_order, is_active, is_featured, max_photos, max_stories, allow_collaborator, allow_custom_domain)
VALUES
    ('FREE', 'Free', 0,
     'Dành cho hai bạn làm quen và tự tay dựng những trang kỷ niệm đầu tiên.',
     '["1 Story tình yêu","Tải lên tối đa 30 ảnh","Template cơ bản","Tên miền couplestory.site"]',
     0, TRUE, FALSE, 30, 1, FALSE, FALSE),

    ('PRO', 'Pro', 49000,
     'Lưu giữ bền lâu với lưu trữ không giới hạn và bảo mật riêng tư.',
     '["Lưu giữ trọn đời không hết hạn","Không giới hạn hình ảnh & video ngắn","Bảo mật mã PIN bảo vệ riêng tư","Nhạc nền tự chọn"]',
     1, TRUE, FALSE, NULL, 1, FALSE, FALSE),

    ('COUPLE', 'Couple', 69000,
     'Cả 2 cùng sở hữu & viết chung kỷ niệm mọi lúc, mọi nơi.',
     '["2 tài khoản cùng quản trị","Dual-presence Live","Timeline cột mốc tình yêu","Bảo mật riêng tư 2 lớp","Không giới hạn ảnh & video"]',
     2, TRUE, TRUE, NULL, 2, TRUE, FALSE),

    ('PRO_MAX', 'Pro Max', 99000,
     'Tuyệt tác cá nhân hóa với tên miền riêng và thiệp cưới điện tử.',
     '["Toàn bộ tính năng gói Couple","Gắn tên miền riêng tùy chỉnh","Xóa hoàn toàn branding CoupleStory","Xuất bản Photobook PDF cao cấp"]',
     3, TRUE, FALSE, NULL, 3, TRUE, TRUE);
