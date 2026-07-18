package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Offerta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListOffersDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        List<Offerta> offers = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    SELECT O.Nome, O.Descrizione, O.Disponibile,
                           NOT EXISTS(
                               SELECT 1
                               FROM CRM.OffertaAccettata AS OA
                               WHERE OA.Offerta = O.Nome
                           ) AS Modificabile
                    FROM CRM.Offerta AS O
                    ORDER BY O.Nome
                    """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Offerta offer = new Offerta();
                offer.setNome(rs.getString("Nome"));
                offer.setDescrizione(rs.getString("Descrizione"));
                offer.setDisponibile(rs.getBoolean("Disponibile"));
                offer.setModificabile(rs.getBoolean("Modificabile"));
                offers.add(offer);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1142) {
                throw new DAOException("L'utente tecnico del database non puo leggere le offerte", e);
            }
            throw new DAOException("Operazione database non riuscita", e);
        }
        return offers;
    }
}
