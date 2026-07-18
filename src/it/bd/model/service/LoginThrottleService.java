package it.bd.model.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginThrottleService {
    private static final int MAX_FAILURES = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 1_000;

    private final Clock clock;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginThrottleService() {
        this(Clock.systemUTC());
    }

    LoginThrottleService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String username, String clientAddress) {
        String key = key(username, clientAddress);
        Instant now = clock.instant();
        AttemptState state = attempts.get(key);
        if (state == null) {
            return false;
        }
        if (state.blockedUntil() != null && state.blockedUntil().isAfter(now)) {
            return true;
        }
        if (!state.windowEndsAt().isAfter(now)
                || state.blockedUntil() != null && !state.blockedUntil().isAfter(now)) {
            attempts.remove(key, state);
        }
        return false;
    }

    public boolean recordFailure(String username, String clientAddress) {
        Instant now = clock.instant();
        cleanupExpired(now);
        AttemptState state = attempts.compute(key(username, clientAddress), (key, current) -> {
            if (current == null || !current.windowEndsAt().isAfter(now)
                    || current.blockedUntil() != null && !current.blockedUntil().isAfter(now)) {
                current = new AttemptState(0, now.plus(ATTEMPT_WINDOW), null);
            }
            if (current.blockedUntil() != null && current.blockedUntil().isAfter(now)) {
                return current;
            }
            int failures = current.failures() + 1;
            Instant blockedUntil = failures >= MAX_FAILURES ? now.plus(BLOCK_DURATION) : null;
            return new AttemptState(failures, current.windowEndsAt(), blockedUntil);
        });
        return state.blockedUntil() != null && state.blockedUntil().isAfter(now);
    }

    public void recordSuccess(String username, String clientAddress) {
        attempts.remove(key(username, clientAddress));
    }

    private void cleanupExpired(Instant now) {
        if (attempts.size() < CLEANUP_THRESHOLD) {
            return;
        }
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private String key(String username, String clientAddress) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String normalizedAddress = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress.trim();
        return normalizedAddress + '\n' + normalizedUsername;
    }

    private record AttemptState(int failures, Instant windowEndsAt, Instant blockedUntil) {
        private boolean isExpired(Instant now) {
            if (blockedUntil != null) {
                return !blockedUntil.isAfter(now);
            }
            return !windowEndsAt.isAfter(now);
        }
    }
}
