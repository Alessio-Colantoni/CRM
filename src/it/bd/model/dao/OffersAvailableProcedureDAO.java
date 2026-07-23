package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Offerta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffersAvailableProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        List<Offerta> offers = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_list_available_offers()}");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                Offerta offer = new Offerta();
                offer.setNome(rs.getString("Nome"));
                offer.setDescrizione(rs.getString("Descrizione"));
                offer.setDisponibile(true);
                offers.add(offer);
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return offers;
    }
}
