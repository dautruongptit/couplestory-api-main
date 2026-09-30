-- Align plans with BA V1.2: FREE / PLUS / COUPLE / PREMIUM (PRO -> PLUS, PRO_MAX -> PREMIUM)

ALTER TABLE users   DROP CONSTRAINT IF EXISTS ck_users_plan_type;
ALTER TABLE stories DROP CONSTRAINT IF EXISTS ck_stories_plan_type;

UPDATE users   SET plan_type = 'PLUS'    WHERE plan_type = 'PRO';
UPDATE users   SET plan_type = 'PREMIUM' WHERE plan_type = 'PRO_MAX';
UPDATE stories SET plan_type = 'PLUS'    WHERE plan_type = 'PRO';
UPDATE stories SET plan_type = 'PREMIUM' WHERE plan_type = 'PRO_MAX';

ALTER TABLE users ADD CONSTRAINT ck_users_plan_type
    CHECK (plan_type IN ('FREE', 'PLUS', 'COUPLE', 'PREMIUM'));
ALTER TABLE stories ADD CONSTRAINT ck_stories_plan_type
    CHECK (plan_type IN ('FREE', 'PLUS', 'COUPLE', 'PREMIUM'));

UPDATE plans SET
    name = 'FREE', price = 0, max_photos = 10, max_stories = 3,
    allow_collaborator = FALSE, allow_custom_domain = FALSE,
    description = 'Bắt đầu miễn phí: tạo website tình yêu đầu tiên của hai bạn.',
    features = '["Tài khoản trọn đời","Website tồn tại 1 tháng","3 website đồng thời","10 ảnh / website","Có watermark CoupleStory"]'::jsonb
WHERE code = 'FREE';

UPDATE plans SET
    code = 'PLUS', name = 'PLUS', price = 49000, max_photos = 100, max_stories = 10,
    allow_collaborator = FALSE, allow_custom_domain = FALSE, is_featured = FALSE,
    description = 'Thêm ảnh, thêm nhạc, bỏ watermark và bảo vệ trang bằng mật khẩu.',
    features = '["Website tồn tại 6 tháng","10 website đồng thời","100 ảnh / website","Không watermark","Đặt mật khẩu cho trang"]'::jsonb
WHERE code = 'PRO';

UPDATE plans SET
    name = 'COUPLE', price = 69000, max_photos = 500, max_stories = 2147483647,
    allow_collaborator = TRUE, allow_custom_domain = FALSE, is_featured = TRUE,
    description = 'Cả hai cùng sở hữu và viết chung kỷ niệm.',
    features = '["Website tồn tại 1 năm","Mời Partner cùng chỉnh sửa","500 ảnh / website","Lời nhắn cho nhau","Chế độ riêng tư"]'::jsonb
WHERE code = 'COUPLE';

UPDATE plans SET
    code = 'PREMIUM', name = 'PREMIUM', price = 119000, max_photos = 1000, max_stories = 2147483647,
    allow_collaborator = TRUE, allow_custom_domain = FALSE, is_featured = FALSE,
    description = 'Website vĩnh viễn, toàn bộ mẫu cao cấp, không cần gia hạn.',
    features = '["Website tồn tại vĩnh viễn","Toàn bộ mẫu cao cấp","1.000+ ảnh / website","20 bài nhạc nền","Toàn bộ quyền lợi gói COUPLE"]'::jsonb
WHERE code = 'PRO_MAX';
