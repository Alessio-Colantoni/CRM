package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchCustomerProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Cliente c = (Cliente) params[0];
        List<Cliente> customerList = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_search_customer_by_name(?,?)}")) {
            cs.setString(1, c.getNome());
            cs.setString(2, c.getCognome());
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    String cf = rs.getString("CF");
                    String name = rs.getString("Nome");
                    String surname = rs.getString("Cognome");
                    Date dateOfBirth = rs.getDate("DataNascita");
                    Date registrationDate = rs.getDate("DataRegistrazione");
                    Date lastInteraction = rs.getDate("DataUltimaInterazione");
                    String address = rs.getString("IndResidenza");
                    Cliente cl = new Cliente();
                    cl.setCodiceFiscale(cf);
                    cl.setNome(name);
                    cl.setCognome(surname);
                    cl.setDataNascita(dateOfBirth);
                    cl.setDataRegistrazione(registrationDate);
                    cl.setDataUltimaInterazione(lastInteraction);
                    cl.setIndirizzoResidenza(address);
                    customerList.add(cl);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return customerList;
    }
}
