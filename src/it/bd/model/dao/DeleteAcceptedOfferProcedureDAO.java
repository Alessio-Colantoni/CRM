package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.OffertaAccettata;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class DeleteAcceptedOfferProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        OffertaAccettata offer = (OffertaAccettata) params[0];
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_delete_customer_accepted_offer(?,?,?)}")) {
            cs.setString(1, offer.getCliente());
            cs.setString(2, offer.getOfferta());
            cs.setDate(3, offer.getData());
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
