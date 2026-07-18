package it.bd.model.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {
    @Test
    void hashesAndVerifiesPassword() {
        String hash = PasswordService.hash("password-sicura");

        assertTrue(PasswordService.matches("password-sicura", hash));
        assertFalse(PasswordService.matches("password-errata", hash));
        assertFalse(PasswordService.matches("password-sicura", "password-sicura"));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordService.hash("corta"));
    }
}
