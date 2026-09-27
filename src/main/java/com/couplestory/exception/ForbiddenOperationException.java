package com.couplestory.exception;

/**
 * Thrown when an authenticated user attempts to act on a resource they do not own
 * or otherwise do not have permission to access. Maps to HTTP 403.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
