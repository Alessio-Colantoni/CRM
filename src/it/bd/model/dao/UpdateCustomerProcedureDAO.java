package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Cliente;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class UpdateCustomerProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Cliente customer = (Cliente) params[0];
        String telephone = (String) params[1];
        String email = (String) params[2];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_update_customer(?,?,?,?,?,?,?)}")) {
            cs.setString(1, customer.getCodiceFiscale());
            cs.setDate(2, customer.getDataNascita());
            cs.setString(3, customer.getNome());
            cs.setString(4, customer.getCognome());
            cs.setString(5, customer.getIndirizzoResidenza());
            cs.setString(6, telephone);
            cs.setString(7, email);
            cs.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new DAOException(e.getMessage(), e);
            }
            if (e.getErrorCode() == 1305) {
                throw new DAOException("La procedura CRM.sp_update_customer non e disponibile nel database configurato", e);
            }
            if (e.getErrorCode() == 1370) {
                throw new DAOException("L'utente tecnico del database non puo eseguire la procedura CRM.sp_update_customer", e);
            }
            if (e.getErrorCode() == 1062) {
                throw new DAOException("Il telefono o l'email indicati risultano gia registrati", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
