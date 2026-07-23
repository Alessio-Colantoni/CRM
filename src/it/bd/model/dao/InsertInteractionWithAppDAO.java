package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Interazione;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class InsertInteractionWithAppDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Interazione i = (Interazione) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_create_interaction_with_appointment(?,?,?,?,?,?)}")) {
            cs.setString(1, i.getNota());
            cs.setDate(2, i.getData());
            cs.setString(3, i.getCliente());
            cs.setString(4, i.getAppuntamento().getSede().getIndirizzo());
            cs.setDate(5, i.getAppuntamento().getData());
            cs.setTime(6, i.getAppuntamento().getOra());
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
