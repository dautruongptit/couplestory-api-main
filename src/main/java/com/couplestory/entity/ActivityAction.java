package com.couplestory.entity;

public enum ActivityAction {
    STORY_CREATED, STORY_UPDATED, STORY_PUBLISHED, STORY_UNPUBLISHED, STORY_DELETED, TEMPLATE_CHANGED,
    EVENT_CREATED, EVENT_UPDATED, EVENT_DELETED,
    PHOTO_UPLOADED, PHOTO_DELETED,
    ORDER_CREATED, ORDER_PAID,
    LOGIN;

    /** Editor saves fire these repeatedly; the timeline keeps one per entity per window. */
    public boolean isDeduplicated() {
        return this == STORY_UPDATED || this == EVENT_UPDATED || this == PHOTO_UPLOADED || this == PHOTO_DELETED
                || this == ORDER_CREATED;
    }
}
