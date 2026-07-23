package it.bd.model.service;

import it.bd.model.domain.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthServiceTest {
    @Test
    void createsFindsAndInvalidatesSession() {
        AuthService service = new AuthService();
        AuthService.AuthSession session = service.createSession("operatore", Role.OPERATORE);

        assertEquals(session, service.findSession("Bearer " + session.token()));
        assertNull(service.findSession(session.token()));

        service.invalidate("Bearer " + session.token());
        assertNull(service.findSession("Bearer " + session.token()));
    }

    @Test
    void invalidatesAllSessionsForUser() {
        AuthService service = new AuthService();
        AuthService.AuthSession first = service.createSession("utente", Role.SEGRETERIA);
        AuthService.AuthSession second = service.createSession("utente", Role.SEGRETERIA);

        service.invalidateUserSessions("utente");

        assertNull(service.findSession("Bearer " + first.token()));
        assertNull(service.findSession("Bearer " + second.token()));
    }
}
