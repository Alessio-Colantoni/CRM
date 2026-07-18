package it.bd.model.dao;

import it.bd.exception.DAOException;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GenerateReportDAO implements GenericProcedureDAO {
    @Override
    public Object execute(Object... params) throws DAOException {
        Date d1 = (Date) params[0];
        Date d2 = (Date) params[1];
        List<Map<String, Object>> report = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             CallableStatement cs = conn.prepareCall("{call CRM.sp_generate_customer_report(?,?,?)}")) {
            cs.setDate(1, d1);
            cs.setDate(2, d2);
            cs.registerOutParameter(3, Types.INTEGER);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("nome", rs.getString("Nome"));
                    row.put("cognome", rs.getString("Cognome"));
                    row.put("interazioni", rs.getInt("Interazioni"));
                    row.put("offerteAccettate", rs.getInt("OfferteAccettate"));
                    report.add(row);
                }
            }
            Map<String, Object> totals = new LinkedHashMap<>();
            totals.put("totaleClientiContattati", cs.getInt(3));
            report.add(totals);
        } catch (SQLException e) {
            throw new DAOException("Operazione database non riuscita", e);
        }
        return report;
    }
}
