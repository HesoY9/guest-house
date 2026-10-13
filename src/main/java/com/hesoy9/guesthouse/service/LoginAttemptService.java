package com.hesoy9.guesthouse.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Login throttling for the staff sign-in page. After too many wrong passwords in a short
// window, that username (and, more generously, that IP address) is locked out for a while,
// which makes guessing passwords impractical. Kept in memory: a restart clears it, which is fine.
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES_PER_USER = 5;
    static final int MAX_FAILURES_PER_IP = 20;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCKOUT = Duration.ofMinutes(15);

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    // How long this username / IP must still wait, or Duration.ZERO if they may try now.
    public Duration lockedFor(String username, String ip) {
        Instant now = clock.instant();
        Duration user = remaining(userKey(username), now);
        Duration addr = remaining(ipKey(ip), now);
        return user.compareTo(addr) >= 0 ? user : addr;
    }

    public void recordFailure(String username, String ip) {
        Instant now = clock.instant();
        bump(userKey(username), MAX_FAILURES_PER_USER, now);
        bump(ipKey(ip), MAX_FAILURES_PER_IP, now);
        if (attempts.size() > 10_000) {
            attempts.values().removeIf(a -> a.expired(now));
        }
    }

    public void recordSuccess(String username, String ip) {
        attempts.remove(userKey(username));
        attempts.remove(ipKey(ip));
    }

    private Duration remaining(String key, Instant now) {
        Attempts a = attempts.get(key);
        if (a == null || a.lockedUntil == null || !a.lockedUntil.isAfter(now)) {
            return Duration.ZERO;
        }
        return Duration.between(now, a.lockedUntil);
    }

    private void bump(String key, int max, Instant now) {
        attempts.compute(key, (k, a) -> {
            if (a == null || a.windowStart.plus(WINDOW).isBefore(now)
                    || (a.lockedUntil != null && !a.lockedUntil.isAfter(now))) {
                a = new Attempts(now); // start a fresh window
            }
            a.failures++;
            if (a.failures >= max) {
                a.lockedUntil = now.plus(LOCKOUT);
            }
            return a;
        });
    }

    private static String userKey(String username) {
        return "u:" + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
    }

    private static String ipKey(String ip) {
        return "ip:" + (ip == null ? "" : ip);
    }

    private static final class Attempts {
        final Instant windowStart;
        int failures;
        Instant lockedUntil;

        Attempts(Instant windowStart) {
            this.windowStart = windowStart;
        }

        boolean expired(Instant now) {
            Instant end = lockedUntil != null ? lockedUntil : windowStart.plus(WINDOW);
            return end.isBefore(now);
        }
    }
}
