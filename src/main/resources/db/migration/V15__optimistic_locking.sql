-- Architecture Review §2.1: an Owner and Partner can edit the same story at
-- once. Without a version check, two overlapping "Save" requests silently
-- let the later commit erase the earlier one. Adding @Version to Story/
-- StoryEvent/FavoriteMoment/StoryMessage makes Hibernate's UPDATE include
-- `WHERE version = ?`; a request whose row changed underneath it gets 0
-- rows affected and Spring raises ObjectOptimisticLockingFailureException
-- (mapped to 409 EDIT_CONFLICT in GlobalExceptionHandler).

ALTER TABLE stories ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE story_events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE favorite_moments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE story_messages ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
