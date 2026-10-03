-- Add 'SYSTEM' to the allowed notification types for security and system events
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS ck_notifications_type;

ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (type IN (
    'TRIAL_EXPIRING', 'TRIAL_EXPIRED', 'INVITE_RECEIVED',
    'PARTNER_JOINED', 'PARTNER_REMOVED', 'OWNER_TRANSFERRED',
    'SYSTEM'
));
