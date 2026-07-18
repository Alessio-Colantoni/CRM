package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.Sede;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ListAddressProcedureDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        List<Sede> addrList = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_list_office_addresses()}");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                Sede s = new Sede();
                String i = rs.getString("Indirizzo");
                s.setIndirizzo(i);
                addrList.add(s);
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return addrList;
    }
}
