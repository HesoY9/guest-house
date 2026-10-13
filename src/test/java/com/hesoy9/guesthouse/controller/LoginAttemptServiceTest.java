package com.hesoy9.guesthouse.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    // A clock the test can move forward by hand.
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-10-10T08:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private final TestClock clock = new TestClock();
    private final LoginAttemptService service = new LoginAttemptService(clock);

    @Test
    void locksUsernameAfterFiveFailures() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("admin", "1.2.3.4");
            assertEquals(Duration.ZERO, service.lockedFor("admin", "1.2.3.4"), "still allowed after " + (i + 1));
        }
        service.recordFailure("admin", "1.2.3.4");

        assertTrue(service.lockedFor("admin", "1.2.3.4").toMinutes() >= 14);
        // the lock is per username, regardless of letter case or which computer
        assertFalse(service.lockedFor("ADMIN", "9.9.9.9").isZero());
    }

    @Test
    void lockExpiresAfterFifteenMinutes() {
        for (int i = 0; i < 5; i++) service.recordFailure("anna", "1.1.1.1");
        assertFalse(service.lockedFor("anna", "1.1.1.1").isZero());

        clock.advance(Duration.ofMinutes(16));

        assertTrue(service.lockedFor("anna", "1.1.1.1").isZero());
    }

    @Test
    void successfulLoginClearsTheCount() {
        for (int i = 0; i < 4; i++) service.recordFailure("sam", "2.2.2.2");
        service.recordSuccess("sam", "2.2.2.2");
        service.recordFailure("sam", "2.2.2.2");

        assertTrue(service.lockedFor("sam", "2.2.2.2").isZero());
    }

    @Test
    void oneAddressTryingManyUsernamesGetsBlocked() {
        for (int i = 0; i < 20; i++) service.recordFailure("user" + i, "6.6.6.6");

        assertFalse(service.lockedFor("someone-new", "6.6.6.6").isZero());
        assertTrue(service.lockedFor("someone-new", "7.7.7.7").isZero());
    }
}
