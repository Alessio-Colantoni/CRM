package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.OffertaAccettata;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerAcceptedOffersDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        String customerCode = (String) params[0];
        List<OffertaAccettata> offers = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_list_customer_accepted_offers(?)}")) {
            cs.setString(1, customerCode);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    OffertaAccettata offer = new OffertaAccettata();
                    offer.setCliente(rs.getString("Cliente"));
                    offer.setOfferta(rs.getString("Offerta"));
                    offer.setUtente(rs.getString("Utente"));
                    offer.setData(rs.getDate("Data"));
                    offers.add(offer);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return offers;
    }
}
