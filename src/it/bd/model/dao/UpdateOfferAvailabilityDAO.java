package it.bd.model.dao;

import it.bd.exception.DAOException;

import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;

public class UpdateOfferAvailabilityDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        String name = (String) params[0];
        Boolean available = (Boolean) params[1];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_admin_set_offer_availability(?,?)}")) {
            cs.setString(1, name);
            cs.setBoolean(2, available);
            cs.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new DAOException(e.getMessage(), e);
            }
            if (e.getErrorCode() == 1305) {
                throw new DAOException("La procedura CRM.sp_admin_set_offer_availability non e disponibile nel database configurato", e);
            }
            if (e.getErrorCode() == 1370) {
                throw new DAOException("L'utente tecnico del database non puo eseguire la procedura CRM.sp_admin_set_offer_availability", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
