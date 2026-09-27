package com.couplestory.security;

import com.couplestory.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTest {

    @Test
    void blocksAfterFiveFailuresFromSameKey() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String key = "203.0.113.10";

        for (int i = 0; i < 5; i++) {
            limiter.checkAllowed(key); // should not throw yet
            limiter.recordFailure(key);
        }

        assertThrows(TooManyRequestsException.class, () -> limiter.checkAllowed(key));
    }

    @Test
    void successfulLoginResetsTheCounter() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String key = "203.0.113.20";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
        }
        limiter.recordSuccess(key);

        assertDoesNotThrow(() -> limiter.checkAllowed(key));
    }

    @Test
    void differentKeysAreTrackedIndependently() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String attacker = "198.51.100.1";
        String innocentBystander = "198.51.100.2";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(attacker);
        }

        assertThrows(TooManyRequestsException.class, () -> limiter.checkAllowed(attacker));
        assertDoesNotThrow(() -> limiter.checkAllowed(innocentBystander));
    }
}
