package com.couplestory.security;

import com.couplestory.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force guard for /api/auth/login, keyed by client IP.
 *
 * This is a single-instance mitigation: state lives in this process's heap and is not
 * shared across instances. That is sufficient for the current single-instance
 * deployment; if the API is ever scaled horizontally, back this with the Redis
 * instance already used elsewhere in this project instead of in-memory state.
 */
@Component
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 10_000; // crude bound against unbounded memory growth

    private final ConcurrentHashMap<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    private static final class Attempts {
        volatile int count;
        volatile Instant windowStart = Instant.now();
    }

    public void checkAllowed(String key) {
        Attempts a = attemptsByKey.get(key);
        if (a == null) return;
        synchronized (a) {
            if (Duration.between(a.windowStart, Instant.now()).compareTo(WINDOW) > 0) {
                a.count = 0;
                a.windowStart = Instant.now();
                return;
            }
            if (a.count >= MAX_ATTEMPTS) {
                throw new TooManyRequestsException("Too many login attempts. Please try again in a few minutes.");
            }
        }
    }

    public void recordFailure(String key) {
        if (attemptsByKey.size() > MAX_TRACKED_KEYS) {
            attemptsByKey.clear(); // crude eviction; acceptable for a single-instance, low-traffic guard
        }
        Attempts a = attemptsByKey.computeIfAbsent(key, k -> new Attempts());
        synchronized (a) {
            if (Duration.between(a.windowStart, Instant.now()).compareTo(WINDOW) > 0) {
                a.count = 0;
                a.windowStart = Instant.now();
            }
            a.count++;
        }
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }
}
