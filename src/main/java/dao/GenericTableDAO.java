import config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GenericTableDAO {

    public static class TableData {
        public List<String> columns = new ArrayList<>();
        public List<List<Object>> rows = new ArrayList<>();
    }

    public TableData fetchTableData(String tableName, String filterColumn, String filterKeyword, int limit, int offset) throws SQLException {
        TableData data = new TableData();
        StringBuilder query = new StringBuilder("SELECT * FROM ").append(tableName);
        boolean hasFilter = filterColumn != null && !filterColumn.isEmpty() && filterKeyword != null && !filterKeyword.trim().isEmpty();

        if (hasFilter) {
            query.append(" WHERE ").append(filterColumn).append(" LIKE ?");
        }
        query.append(" LIMIT ? OFFSET ?");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            
            int paramIdx = 1;
            if (hasFilter) {
                pstmt.setString(paramIdx++, "%" + filterKeyword.trim() + "%");
            }
            pstmt.setInt(paramIdx++, limit);
            pstmt.setInt(paramIdx++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int colCount = metaData.getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    data.columns.add(metaData.getColumnLabel(i));
                }

                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    data.rows.add(row);
                }
            }
        }
        return data;
    }

    public boolean insertRecord(String tableName, List<String> columns, List<Object> values) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder placeholders = new StringBuilder("(");

        for (int i = 0; i < columns.size(); i++) {
            sql.append(columns.get(i));
            placeholders.append("?");
            if (i < columns.size() - 1) {
                sql.append(", ");
                placeholders.append(", ");
            }
        }
        sql.append(") VALUES ").append(placeholders).append(")");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateRecord(String tableName, List<String> columns, List<Object> values, List<String> pkColumns, List<Object> pkValues) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        for (int i = 0; i < columns.size(); i++) {
            sql.append(columns.get(i)).append(" = ?");
            if (i < columns.size() - 1) sql.append(", ");
        }
        sql.append(" WHERE ");
        for (int i = 0; i < pkColumns.size(); i++) {
            sql.append(pkColumns.get(i)).append(" = ?");
            if (i < pkColumns.size() - 1) sql.append(" AND ");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (Object val : values) pstmt.setObject(idx++, val);
            for (Object pkVal : pkValues) pstmt.setObject(idx++, pkVal);

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteRecord(String tableName, List<String> pkColumns, List<Object> pkValues) throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");
        for (int i = 0; i < pkColumns.size(); i++) {
            sql.append(pkColumns.get(i)).append(" = ?");
            if (i < pkColumns.size() - 1) sql.append(" AND ");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < pkValues.size(); i++) {
                pstmt.setObject(i + 1, pkValues.get(i));
            }
            return pstmt.executeUpdate() > 0;
        }
    }
}