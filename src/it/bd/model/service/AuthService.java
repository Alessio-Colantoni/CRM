package it.bd.model.service;

import it.bd.model.domain.Role;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final long SESSION_TTL_SECONDS = 8 * 60 * 60;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthSession createSession(String username, Role role) {
        removeExpiredSessions();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        AuthSession session = new AuthSession(token, username, role, Instant.now().plusSeconds(SESSION_TTL_SECONDS));
        sessions.put(token, session);
        return session;
    }

    public AuthSession findSession(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring("Bearer ".length());
        AuthSession session = sessions.get(token);
        if (session == null) {
            return null;
        }

        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(token);
            return null;
        }

        return session;
    }

    public void invalidate(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            sessions.remove(authorizationHeader.substring("Bearer ".length()));
        }
    }

    public void invalidateUserSessions(String username) {
        sessions.entrySet().removeIf(entry -> entry.getValue().username().equals(username));
    }

    private void removeExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    public record AuthSession(String token, String username, Role role, Instant expiresAt) {
    }
}
