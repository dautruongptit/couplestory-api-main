package com.couplestory.exception;

/** Thrown when a client exceeds a rate limit (e.g. repeated failed logins). Maps to HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyRequestsException(String message) {
        this(message, 0);
    }

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** 0 when unknown. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
