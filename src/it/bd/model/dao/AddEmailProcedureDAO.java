package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.*;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class AddEmailProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Cliente c = (Cliente) params[0];
        Email e = (Email) params[1];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_add_customer_email(?,?)}")) {
            cs.setString(1, e.getIndirizzoEmail());
            cs.setString(2, c.getCodiceFiscale());
            cs.executeUpdate();
        } catch (SQLException ex) {
            throw new DAOException("Operazione database non riuscita", ex);
        }
        return null;
    }
}
