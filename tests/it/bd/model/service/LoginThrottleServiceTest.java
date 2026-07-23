package it.bd.model.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginThrottleServiceTest {
    @Test
    void blocksAfterFiveFailuresAndSuccessClearsAttempts() {
        LoginThrottleService service = new LoginThrottleService(
                Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneId.of("UTC")));

        for (int attempt = 1; attempt < 5; attempt++) {
            assertFalse(service.recordFailure("utente", "127.0.0.1"));
        }
        assertTrue(service.recordFailure("utente", "127.0.0.1"));
        assertTrue(service.isBlocked("utente", "127.0.0.1"));

        service.recordSuccess("utente", "127.0.0.1");
        assertFalse(service.isBlocked("utente", "127.0.0.1"));
    }

    @Test
    void keepsAttemptsSeparateByUserAndClient() {
        LoginThrottleService service = new LoginThrottleService(
                Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneId.of("UTC")));

        for (int attempt = 0; attempt < 5; attempt++) {
            service.recordFailure("utente", "127.0.0.1");
        }

        assertFalse(service.isBlocked("altro-utente", "127.0.0.1"));
        assertFalse(service.isBlocked("utente", "127.0.0.2"));
    }
}
