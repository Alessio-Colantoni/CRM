package it.bd.model.dao;

import it.bd.exception.DAOException;

import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;

public class DeleteOfferDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        String name = (String) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_admin_delete_offer(?)}")) {
            cs.setString(1, name);
            cs.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new DAOException(e.getMessage(), e);
            }
            if (e.getErrorCode() == 1305) {
                throw new DAOException("La procedura CRM.sp_admin_delete_offer non e disponibile nel database configurato", e);
            }
            if (e.getErrorCode() == 1370) {
                throw new DAOException("L'utente tecnico del database non puo eseguire la procedura CRM.sp_admin_delete_offer", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
