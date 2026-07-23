package it.bd.model.dao;

import it.bd.exception.DAOException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class DeleteAppointmentProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        int interactionCode = (Integer) params[0];
        String customerCode = (String) params[1];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_delete_customer_appointment(?,?)}")) {
            cs.setInt(1, interactionCode);
            cs.setString(2, customerCode);
            cs.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new DAOException(e.getMessage(), e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
