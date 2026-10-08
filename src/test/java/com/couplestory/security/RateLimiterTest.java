package com.couplestory.security;

import com.couplestory.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    private static final RatePolicy THREE_PER_MINUTE = new RatePolicy("TEST", 3, Duration.ofMinutes(1));

    /** A clock the test can move. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void allowsUpToTheLimitThenBlocks() {
        for (int i = 0; i < 3; i++) limiter.check(THREE_PER_MINUTE, "1.2.3.4");

        assertThatThrownBy(() -> limiter.check(THREE_PER_MINUTE, "1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void theRejectionCarriesTheSecondsUntilTheWindowEnds() {
        for (int i = 0; i < 3; i++) limiter.check(THREE_PER_MINUTE, "k");
        clock.advance(Duration.ofSeconds(20));

        assertThatThrownBy(() -> limiter.check(THREE_PER_MINUTE, "k"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(40));
    }

    @Test
    void aRejectedAttemptDoesNotExtendTheBlock() {
        for (int i = 0; i < 3; i++) limiter.check(THREE_PER_MINUTE, "k");
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> limiter.check(THREE_PER_MINUTE, "k")).isInstanceOf(TooManyRequestsException.class);
        }
        clock.advance(Duration.ofSeconds(61));

        assertThatCode(() -> limiter.check(THREE_PER_MINUTE, "k")).doesNotThrowAnyException();
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) limiter.check(THREE_PER_MINUTE, "noisy-user");

        assertThatCode(() -> limiter.check(THREE_PER_MINUTE, "quiet-user")).doesNotThrowAnyException();
    }

    @Test
    void policiesAreIndependentForTheSameKey() {
        RatePolicy other = new RatePolicy("OTHER", 3, Duration.ofMinutes(1));
        for (int i = 0; i < 3; i++) limiter.check(THREE_PER_MINUTE, "k");

        assertThatCode(() -> limiter.check(other, "k")).doesNotThrowAnyException();
    }

    @Test
    void retryAfterIsAtLeastOneSecond() {
        for (int i = 0; i < 3; i++) limiter.check(THREE_PER_MINUTE, "k");
        clock.advance(Duration.ofMillis(59_900));

        assertThatThrownBy(() -> limiter.check(THREE_PER_MINUTE, "k"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isGreaterThanOrEqualTo(1));
    }

    @Test
    void expiredEntriesAreEvictedSoMemoryStaysBounded() {
        for (int i = 0; i < RateLimiter.MAX_TRACKED_KEYS + 5; i++) limiter.check(THREE_PER_MINUTE, "k" + i);
        clock.advance(Duration.ofMinutes(2));

        limiter.check(THREE_PER_MINUTE, "fresh");

        assertThat(limiter.trackedKeys()).isLessThan(100);
    }
}
