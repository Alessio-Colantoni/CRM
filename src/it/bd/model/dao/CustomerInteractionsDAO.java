package it.bd.model.dao;

import it.bd.exception.DAOException;
import it.bd.model.domain.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerInteractionsDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Cliente c = (Cliente) params[0];
        List<Interazione> interactionsList = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_list_customer_interactions(?)}")) {
            cs.setString(1, c.getCodiceFiscale());
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    String note = rs.getString("Nota");
                    Date interactionDate = rs.getDate("DataInterazione");
                    String address = rs.getString("IndirizzoAppuntamento");
                    Date dateApp = rs.getDate("DataAppuntamento");
                    Time hourApp = rs.getTime("OraAppuntamento");
                    Interazione i = new Interazione();
                    Appuntamento a = new Appuntamento();
                    Sede s = new Sede();
                    s.setIndirizzo(address);
                    a.setSede(s);
                    a.setData(dateApp);
                    a.setOra(hourApp);
                    i.setCodiceInterazione(rs.getInt("CodInterazione"));
                    i.setNota(note);
                    i.setData(interactionDate);
                    i.setAppuntamento(a);
                    interactionsList.add(i);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return interactionsList;
    }
}
