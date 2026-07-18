package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Cliente;
import it.bd.model.domain.Telefono;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class AddTelProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Cliente c = (Cliente) params[0];
        Telefono t = (Telefono) params[1];

        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_add_customer_telephone(?,?)}")) {
            cs.setString(1, t.getNumero());
            cs.setString(2, c.getCodiceFiscale());
            cs.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return null;
    }
}
