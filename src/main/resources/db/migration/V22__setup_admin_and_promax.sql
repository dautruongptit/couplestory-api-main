-- Grant ADMIN role to dautruongptit@gmail.com
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'dautruongptit@gmail.com' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Set promax@couplestory.site to PRO_MAX plan
UPDATE users SET plan_type = 'PRO_MAX' WHERE email = 'promax@couplestory.site';
