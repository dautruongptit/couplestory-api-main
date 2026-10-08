package com.couplestory.security;

import com.couplestory.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window request limiter. Single-instance by design (same trade-off as
 * LoginRateLimiter): back it with Redis if the API is ever scaled horizontally.
 * Rejected calls are not counted, so hammering a blocked endpoint does not extend the block.
 */
@Component
public class RateLimiter {

    static final int MAX_TRACKED_KEYS = 10_000;

    private static final class Window {
        final Instant end;
        int count;

        Window(Instant end) {
            this.end = end;
        }
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(RatePolicy policy, String key) {
        Instant now = clock.instant();
        if (windows.size() > MAX_TRACKED_KEYS) windows.values().removeIf(w -> !now.isBefore(w.end));
        Window w = windows.compute(policy.name() + ":" + key, (k, current) ->
                current == null || !now.isBefore(current.end) ? new Window(now.plus(policy.window())) : current);
        synchronized (w) {
            if (w.count >= policy.maxRequests()) {
                long seconds = (long) Math.ceil(Duration.between(now, w.end).toMillis() / 1000.0);
                throw new TooManyRequestsException("Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút.", Math.max(1, seconds));
            }
            w.count++;
        }
    }

    int trackedKeys() {
        return windows.size();
    }

    /** Test hook. */
    public void clear() {
        windows.clear();
    }
}
