-- Carry forward from old V2__add_title_and_letter_fields: these fields are
-- used by StoryEditor (hero short quote) and the love-letter editor (heading
-- and signature) but were not included in V14's structured content migration.

ALTER TABLE stories ADD COLUMN short_quote VARCHAR(500);

ALTER TABLE story_messages ADD COLUMN heading VARCHAR(255);
ALTER TABLE story_messages ADD COLUMN signature VARCHAR(255);
