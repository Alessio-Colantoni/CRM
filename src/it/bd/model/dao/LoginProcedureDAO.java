package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Credenziali;
import it.bd.model.domain.Role;
import it.bd.model.security.PasswordService;

import java.sql.*;

public class LoginProcedureDAO implements GenericProcedureDAO<Credenziali> {

    @Override
    public Credenziali execute(Object... params) throws DAOException {
        String username = (String) params[0];
        String password = (String) params[1];
        Role role = null;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    SELECT C.Password, U.Ruolo
                    FROM CRM.Credenziali AS C
                    JOIN CRM.Utente AS U ON U.ID = C.Username
                    WHERE C.Username = ?
                    """)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("Password");
                    if (PasswordService.matches(password, storedPassword)) {
                        role = roleFromDb(rs.getString("Ruolo"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }

        return new Credenziali(username, role);
    }

    private Role roleFromDb(String role) {
        if (role == null) {
            return null;
        }
        return Role.valueOf(role.toUpperCase());
    }

}
