package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Cliente;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class AddCustomerProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Cliente c = (Cliente) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_create_customer(?,?,?,?,?,?)}")) {
            cs.setString(1, c.getCodiceFiscale());
            cs.setDate(2, c.getDataNascita());
            cs.setString(3, c.getNome());
            cs.setString(4, c.getCognome());
            cs.setDate(5, c.getDataRegistrazione());
            cs.setString(6, c.getIndirizzoResidenza());
            cs.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DAOException("Esiste gia un cliente con questo codice fiscale", e);
            }
            if (e.getErrorCode() == 1305) {
                throw new DAOException("La procedura CRM.sp_create_customer non e disponibile nel database configurato", e);
            }
            if (e.getErrorCode() == 1370) {
                throw new DAOException("L'utente tecnico del database non puo eseguire la procedura CRM.sp_create_customer", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
