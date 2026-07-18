package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerListProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        List<Cliente> customers = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_list_operator_customers()}");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                Cliente customer = new Cliente();
                customer.setNome(rs.getString("Nome"));
                customer.setCognome(rs.getString("Cognome"));
                customers.add(customer);
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return customers;
    }
}
