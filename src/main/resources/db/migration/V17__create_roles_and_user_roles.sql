-- Separate roles table with many-to-many user_roles
CREATE TABLE roles (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(20) NOT NULL
);

CREATE UNIQUE INDEX ux_roles_name ON roles (name);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seed default roles
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

-- Migrate existing users.role data into user_roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = u.role;

-- Drop the old single-value column
ALTER TABLE users DROP COLUMN role;
