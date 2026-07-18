package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Cliente;
import it.bd.model.domain.Telefono;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchCustomerTelephoneDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Cliente c = (Cliente) params[0];
        List<Telefono> telList = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_list_customer_telephones(?)}")) {
            cs.setString(1, c.getCodiceFiscale());
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Telefono n = new Telefono();
                    n.setNumero(rs.getString("Numero"));
                    telList.add(n);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return telList;
    }
}
