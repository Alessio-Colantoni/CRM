package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.security.PasswordService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChangePasswordDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        String username = (String) params[0];
        String oldPassword = (String) params[1];
        String newPassword = (String) params[2];
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement select = conn.prepareStatement("""
                        SELECT Password
                        FROM CRM.Credenziali
                        WHERE Username = ?
                        FOR UPDATE
                        """)) {
                    select.setString(1, username);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next() || !PasswordService.matches(oldPassword, rs.getString("Password"))) {
                            throw new DAOException("Password attuale non valida");
                        }
                    }
                }

                try (PreparedStatement update = conn.prepareStatement("""
                        UPDATE CRM.Credenziali
                        SET Password = ?
                        WHERE Username = ?
                        """)) {
                    update.setString(1, PasswordService.hash(newPassword));
                    update.setString(2, username);
                    if (update.executeUpdate() == 0) {
                        throw new DAOException("Password attuale non valida");
                    }
                }
                conn.commit();
            } catch (SQLException | DAOException | RuntimeException exception) {
                conn.rollback();
                throw exception;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
