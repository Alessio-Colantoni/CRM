package it.bd.model.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionFactory {
    private static volatile HikariDataSource dataSource;

    private ConnectionFactory() {
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    public static boolean isAvailable() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private static HikariDataSource dataSource() throws SQLException {
        HikariDataSource current = dataSource;
        if (current != null) {
            return current;
        }
        synchronized (ConnectionFactory.class) {
            if (dataSource == null) {
                dataSource = createDataSource();
            }
            return dataSource;
        }
    }

    private static HikariDataSource createDataSource() throws SQLException {
        String connectionUrl = configured("CONNECTION_URL");
        String user = configured("DB_USER");
        String pass = configured("DB_PASSWORD");

        if (connectionUrl == null || user == null || pass == null) {
            throw new SQLException("Database configuration is incomplete. Configure CONNECTION_URL, DB_USER and DB_PASSWORD in the deployment environment.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionUrl);
        config.setUsername(user);
        config.setPassword(pass);
        config.setPoolName("crm-db-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setValidationTimeout(3_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_500_000);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private static String configured(String key) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? null : value;
    }
}
