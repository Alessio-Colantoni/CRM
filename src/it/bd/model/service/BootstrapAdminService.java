package it.bd.model.service;

import it.bd.model.dao.ConnectionFactory;
import it.bd.model.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BootstrapAdminService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapAdminService.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String username = configured("BOOTSTRAP_ADMIN_USERNAME");
        String password = configured("BOOTSTRAP_ADMIN_PASSWORD");
        if (username == null && password == null) {
            return;
        }
        if (username == null) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_USERNAME e BOOTSTRAP_ADMIN_PASSWORD devono essere configurate insieme");
        }
        if (password == null) {
            if (existsAndIsConsistent(username)) {
                return;
            }
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD e necessaria finche l'amministratore iniziale non e stato creato");
        }

        String name = configuredOrDefault("BOOTSTRAP_ADMIN_NAME", "Initial");
        String surname = configuredOrDefault("BOOTSTRAP_ADMIN_SURNAME", "Admin");
        createIfMissing(username, password, name, surname);
    }

    private boolean existsAndIsConsistent(String username) throws SQLException {
        try (Connection connection = ConnectionFactory.getConnection()) {
            int credentials = count(connection, "SELECT COUNT(*) FROM CRM.Credenziali WHERE Username = ?", username);
            int users = count(connection,
                    "SELECT COUNT(*) FROM CRM.Utente WHERE ID = ? AND Ruolo = 'amministratore'", username);
            if (credentials == 1 && users == 1) {
                return true;
            }
            if (credentials != 0 || users != 0) {
                throw new SQLException("Dati bootstrap amministratore incoerenti per " + username);
            }
            return false;
        }
    }

    private void createIfMissing(String username, String password, String name, String surname) throws SQLException {
        try (Connection connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int credentials = count(connection, "SELECT COUNT(*) FROM CRM.Credenziali WHERE Username = ?", username);
                int users = count(connection,
                        "SELECT COUNT(*) FROM CRM.Utente WHERE ID = ? AND Ruolo = 'amministratore'", username);
                if (credentials == 1 && users == 1) {
                    connection.commit();
                    return;
                }
                if (credentials != 0 || users != 0) {
                    throw new SQLException("Dati bootstrap amministratore incoerenti per " + username);
                }

                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO CRM.Credenziali(Username, Password)
                        VALUES (?, ?)
                        """)) {
                    statement.setString(1, username);
                    statement.setString(2, PasswordService.hash(password));
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO CRM.Utente(ID, Ruolo, Nome, Cognome)
                        VALUES (?, 'amministratore', ?, ?)
                        """)) {
                    statement.setString(1, username);
                    statement.setString(2, name);
                    statement.setString(3, surname);
                    statement.executeUpdate();
                }
                connection.commit();
                logger.warn("Amministratore iniziale creato. Rimuovere subito la password di bootstrap dall'ambiente.");
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int count(Connection connection, String query, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private String configuredOrDefault(String key, String defaultValue) {
        String value = configured(key);
        return value == null ? defaultValue : value;
    }

    private String configured(String key) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
