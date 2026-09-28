-- Update plan types: TRIAL→FREE, PERMANENT→FREE, COUPLE stays, PRO stays
-- Final plans: FREE, PRO, COUPLE, PRO_MAX

-- Drop old constraint
ALTER TABLE stories DROP CONSTRAINT ck_stories_plan_type;

-- Migrate existing data
UPDATE stories SET plan_type = 'FREE' WHERE plan_type IN ('TRIAL', 'PERMANENT');

-- Add new constraint with updated plan types
ALTER TABLE stories
    ADD CONSTRAINT ck_stories_plan_type
        CHECK (plan_type IN ('FREE', 'PRO', 'COUPLE', 'PRO_MAX'));

-- Update trial expiry index (FREE replaces TRIAL)
DROP INDEX IF EXISTS ix_stories_trial_expiry;
CREATE INDEX ix_stories_free_expiry ON stories (expires_at)
    WHERE plan_type = 'FREE' AND status = 'PUBLISHED';
