package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Utente;
import it.bd.model.security.PasswordService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddUserProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Utente u = (Utente) params[0];
        String password = (String) params[1];
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement credentials = conn.prepareStatement("""
                        INSERT INTO CRM.Credenziali(Username, Password)
                        VALUES (?, ?)
                        """)) {
                    credentials.setString(1, u.getId());
                    credentials.setString(2, PasswordService.hash(password));
                    credentials.executeUpdate();
                }

                try (PreparedStatement user = conn.prepareStatement("""
                        INSERT INTO CRM.Utente(ID, Ruolo, Nome, Cognome)
                        VALUES (?, ?, ?, ?)
                        """)) {
                    user.setString(1, u.getId());
                    user.setString(2, u.getRuolo());
                    user.setString(3, u.getNome());
                    user.setString(4, u.getCognome());
                    user.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
