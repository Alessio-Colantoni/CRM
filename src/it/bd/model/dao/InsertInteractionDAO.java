package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Interazione;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class InsertInteractionDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Interazione i = (Interazione) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_create_interaction(?,?,?)}")) {
            cs.setString(1, i.getNota());
            cs.setDate(2, i.getData());
            cs.setString(3, i.getCliente());
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
