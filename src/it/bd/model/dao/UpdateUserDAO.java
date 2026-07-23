package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateUserDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Utente user = (Utente) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    UPDATE CRM.Utente
                    SET Ruolo = ?, Nome = ?, Cognome = ?
                    WHERE ID = ?
                    """)) {
            ps.setString(1, user.getRuolo());
            ps.setString(2, user.getNome());
            ps.setString(3, user.getCognome());
            ps.setString(4, user.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DAOException("Utente non trovato");
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
