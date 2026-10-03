-- Story events (saved / published) notify the owner; the UI already renders the STORY type.
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS ck_notifications_type;

ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (type IN (
    'TRIAL_EXPIRING', 'TRIAL_EXPIRED', 'INVITE_RECEIVED',
    'PARTNER_JOINED', 'PARTNER_REMOVED', 'OWNER_TRANSFERRED',
    'SYSTEM', 'STORY'
));
