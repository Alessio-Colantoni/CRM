package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListUsersDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        List<Utente> users = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    SELECT ID, Ruolo, Nome, Cognome
                    FROM CRM.Utente
                    ORDER BY Ruolo, Cognome, Nome
                    """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Utente user = new Utente();
                user.setId(rs.getString("ID"));
                user.setRuolo(rs.getString("Ruolo"));
                user.setNome(rs.getString("Nome"));
                user.setCognome(rs.getString("Cognome"));
                users.add(user);
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return users;
    }
}
