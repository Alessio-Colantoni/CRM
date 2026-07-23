package it.bd.model.dao;

import it.bd.exception.DAOException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class DeleteCustomerProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        String codiceFiscale = (String) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_delete_customer(?)}")) {
            cs.setString(1, codiceFiscale);
            cs.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new DAOException(e.getMessage(), e);
            }
            if (e.getErrorCode() == 1305) {
                throw new DAOException("La procedura CRM.sp_delete_customer non e disponibile nel database configurato", e);
            }
            if (e.getErrorCode() == 1370) {
                throw new DAOException("L'utente tecnico del database non puo eseguire la procedura CRM.sp_delete_customer", e);
            }
            if (e.getErrorCode() == 1451) {
                throw new DAOException("Impossibile eliminare il cliente: esistono dati collegati non gestiti dalla procedura", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
