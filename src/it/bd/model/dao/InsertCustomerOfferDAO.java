package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.OffertaAccettata;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class InsertCustomerOfferDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        OffertaAccettata oa = (OffertaAccettata) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_register_accepted_offer(?,?,?,?)}")) {
            cs.setString(1, oa.getCliente());
            cs.setString(2, oa.getOfferta());
            cs.setString(3, oa.getUtente());
            cs.setDate(4, oa.getData());
            cs.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new DAOException(e.getMessage(), e);
            }
            if (e.getErrorCode() == 1062) {
                throw new DAOException("Esiste gia un'offerta accettata per questo cliente, questa data e questa offerta", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
