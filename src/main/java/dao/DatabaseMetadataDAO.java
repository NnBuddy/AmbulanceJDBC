import config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class DatabaseMetadataDAO {

    public static class ColumnInfo {
        public String name;
        public String typeName;
        public int columnSize;
        public boolean isNullable;
        public boolean isAutoIncrement;
        public String defaultValue;

        public ColumnInfo(String name, String typeName, int columnSize, boolean isNullable, boolean isAutoIncrement, String defaultValue) {
            this.name = name;
            this.typeName = typeName;
            this.columnSize = columnSize;
            this.isNullable = isNullable;
            this.isAutoIncrement = isAutoIncrement;
            this.defaultValue = defaultValue;
        }
    }

    public List<String> getTableNames() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        Collections.sort(tables);
        return tables;
    }

    public List<ColumnInfo> getTableColumns(String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, "%")) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int size = rs.getInt("COLUMN_SIZE");
                boolean nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                boolean autoInc = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                String defVal = rs.getString("COLUMN_DEF");
                columns.add(new ColumnInfo(colName, typeName, size, nullable, autoInc, defVal));
            }
        }
        return columns;
    }

    public List<String> getPrimaryKeys(String tableName) throws SQLException {
        List<String> pks = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             ResultSet rs = conn.getMetaData().getPrimaryKeys(conn.getCatalog(), null, tableName)) {
            while (rs.next()) {
                pks.add(rs.getString("COLUMN_NAME"));
            }
        }
        return pks;
    }

    public Map<String, Long> getDashboardCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        String[] tables = {"Caller", "Incident", "Ambulance", "Hospital", "Dispatch"};
        for (String table : tables) {
            String query = "SELECT COUNT(*) FROM " + table;
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    counts.put(table, rs.getLong(1));
                }
            } catch (Exception e) {
                counts.put(table, 0L);
            }
        }
        return counts;
    }
}