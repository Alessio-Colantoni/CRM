package it.bd.model.dao;

import it.bd.exception.DAOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeleteUserDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        String id = (String) params[0];
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement acceptedOffers = conn.prepareStatement("""
                            SELECT COUNT(*)
                            FROM CRM.OffertaAccettata
                            WHERE Utente = ?
                            """)) {
                    acceptedOffers.setString(1, id);
                    try (ResultSet offers = acceptedOffers.executeQuery()) {
                        if (offers.next() && offers.getInt(1) > 0) {
                            throw new DAOException("Impossibile rimuovere l'utente: risulta associato a offerte accettate");
                        }
                    }
                }

                try (PreparedStatement deleteUser = conn.prepareStatement("""
                            DELETE FROM CRM.Utente
                            WHERE ID = ?
                            """)) {
                    deleteUser.setString(1, id);
                    if (deleteUser.executeUpdate() == 0) {
                        throw new DAOException("Utente non trovato");
                    }
                }

                try (PreparedStatement deleteCredentials = conn.prepareStatement("""
                            DELETE FROM CRM.Credenziali
                            WHERE Username = ?
                            """)) {
                    deleteCredentials.setString(1, id);
                    deleteCredentials.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | DAOException e) {
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
