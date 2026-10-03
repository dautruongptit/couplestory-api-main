package com.couplestory.exception;

/** Thrown when a requested story link is already used by another story. Maps to HTTP 409. */
public class SlugTakenException extends RuntimeException {
    public SlugTakenException(String message) {
        super(message);
    }
}
