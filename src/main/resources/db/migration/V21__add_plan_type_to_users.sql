ALTER TABLE users ADD COLUMN plan_type VARCHAR(20) NOT NULL DEFAULT 'FREE';

ALTER TABLE users ADD CONSTRAINT ck_users_plan_type
    CHECK (plan_type IN ('FREE', 'PRO', 'COUPLE', 'PRO_MAX'));
