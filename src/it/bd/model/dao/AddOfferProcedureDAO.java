package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Offerta;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class AddOfferProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Offerta o = (Offerta) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_admin_create_offer(?,?,?)}")) {
            cs.setString(1, o.getNome());
            cs.setString(2, o.getDescrizione());
            cs.setBoolean(3, o.isDisponibile());
            cs.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DAOException("Esiste gia un'offerta con questo nome", e);
            }
            if (e.getErrorCode() == 1305) {
                throw new DAOException("La procedura CRM.sp_admin_create_offer non e disponibile nel database configurato", e);
            }
            if (e.getErrorCode() == 1370) {
                throw new DAOException("L'utente tecnico del database non puo eseguire la procedura CRM.sp_admin_create_offer", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
