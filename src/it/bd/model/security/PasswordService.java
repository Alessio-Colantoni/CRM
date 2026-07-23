package it.bd.model.security;

import org.springframework.security.crypto.bcrypt.BCrypt;

public final class PasswordService {
    private static final int COST = 12;

    private PasswordService() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La password non puo essere vuota");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("La password deve contenere almeno 8 caratteri");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (!isBcrypt(storedPassword)) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, storedPassword);
    }

    private static boolean isBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
