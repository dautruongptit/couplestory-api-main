-- Drop the old constraint that restricted template_code to just 'minimal-couple' and 'eternal-love'
-- Since V24 introduced the templates table, we should allow any template code that exists.
ALTER TABLE stories DROP CONSTRAINT IF EXISTS ck_stories_template_code;
