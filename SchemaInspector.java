import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * SchemaInspector - READ-ONLY utility.
 * Prints tables, views, columns, primary keys, foreign keys, row counts and the
 * distinct values of any column whose name contains "status" or "state".
 * It never modifies the database and never prints the password.
 *
 * Output is shown on screen and saved to schema-report.txt.
 */
public class SchemaInspector {

    private static final StringBuilder OUT = new StringBuilder();

    private static void p(String line) {
        System.out.println(line);
        OUT.append(line).append('\n');
    }

    /** Safely quote a MySQL identifier. */
    private static String q(String id) {
        return "`" + id.replace("`", "``") + "`";
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("Could not read config.properties in the current folder: "
                    + System.getProperty("user.dir"));
            System.err.println("Copy config.example.properties to config.properties and fill in your password.");
            return;
        }

        String url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/AmbulanceDispatchDB").trim();
        String user = props.getProperty("db.user", "root").trim();
        String password = props.getProperty("db.password", "");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver not found. Make sure mysql-connector-j-26.7.0.jar is on the classpath.");
            return;
        }

        try (Connection con = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData md = con.getMetaData();
            String catalog = con.getCatalog();

            p("=== CONNECTION ===");
            p("URL      : " + url);
            p("User     : " + user);
            p("Database : " + catalog);
            p("Server   : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            p("Driver   : " + md.getDriverName() + " " + md.getDriverVersion());
            p("");

            List<String[]> tables = new ArrayList<>();
            try (ResultSet rs = md.getTables(catalog, null, "%", new String[] {"TABLE", "VIEW"})) {
                while (rs.next()) {
                    tables.add(new String[] {rs.getString("TABLE_NAME"), rs.getString("TABLE_TYPE")});
                }
            }

            p("=== TABLES AND VIEWS FOUND: " + tables.size() + " ===");
            for (String[] t : tables) {
                p(" - " + t[0] + " (" + t[1] + ")");
            }
            p("");

            for (String[] t : tables) {
                describe(con, md, catalog, t[0], t[1]);
            }

            p("=== END OF REPORT ===");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            System.err.println("SQLState=" + e.getSQLState() + "  ErrorCode=" + e.getErrorCode());
            return;
        }

        try {
            Files.write(Paths.get("schema-report.txt"), OUT.toString().getBytes("UTF-8"));
            System.out.println();
            System.out.println("Report also saved to: " + Paths.get("schema-report.txt").toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not save schema-report.txt: " + e.getMessage());
        }
    }

    private static void describe(Connection con, DatabaseMetaData md, String catalog,
                                 String table, String type) throws SQLException {
        p("------------------------------------------------------------");
        p("TABLE: " + table + "   [" + type + "]");
        p("------------------------------------------------------------");

        // Columns
        List<String> statusColumns = new ArrayList<>();
        p("Columns:");
        try (ResultSet rs = md.getColumns(catalog, null, table, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String lower = name.toLowerCase();
                if (lower.contains("status") || lower.contains("state")) {
                    statusColumns.add(name);
                }
                p(String.format("  %-25s %-14s size=%-6s nullable=%-3s autoInc=%-3s default=%s",
                        name,
                        rs.getString("TYPE_NAME"),
                        rs.getString("COLUMN_SIZE"),
                        rs.getString("IS_NULLABLE"),
                        rs.getString("IS_AUTOINCREMENT"),
                        rs.getString("COLUMN_DEF")));
            }
        }

        // Primary key (ordered by KEY_SEQ)
        Map<Integer, String> pk = new TreeMap<>();
        try (ResultSet rs = md.getPrimaryKeys(catalog, null, table)) {
            while (rs.next()) {
                pk.put(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME"));
            }
        }
        p("Primary key: " + (pk.isEmpty() ? "NONE" : String.join(", ", pk.values())));

        // Foreign keys
        p("Foreign keys (this table -> referenced table):");
        boolean anyFk = false;
        try (ResultSet rs = md.getImportedKeys(catalog, null, table)) {
            while (rs.next()) {
                anyFk = true;
                p("  " + rs.getString("FK_NAME") + ": " + rs.getString("FKCOLUMN_NAME")
                        + " -> " + rs.getString("PKTABLE_NAME") + "." + rs.getString("PKCOLUMN_NAME")
                        + "  (ON DELETE rule=" + ruleName(rs.getShort("DELETE_RULE"))
                        + ", ON UPDATE rule=" + ruleName(rs.getShort("UPDATE_RULE")) + ")");
            }
        }
        if (!anyFk) {
            p("  none");
        }

        // Row count
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + q(table))) {
            if (rs.next()) {
                p("Row count: " + rs.getLong(1));
            }
        } catch (SQLException e) {
            p("Row count: could not be read (" + e.getMessage() + ")");
        }

        // Distinct values of status-like columns
        for (String col : statusColumns) {
            String sql = "SELECT DISTINCT " + q(col) + " FROM " + q(table) + " LIMIT 20";
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (rs.next()) {
                    values.add(String.valueOf(rs.getString(1)));
                }
                p("Distinct values in " + col + " (max 20): " + values);
            } catch (SQLException e) {
                p("Distinct values in " + col + ": could not be read (" + e.getMessage() + ")");
            }
        }
        p("");
    }

    private static String ruleName(short rule) {
        switch (rule) {
            case DatabaseMetaData.importedKeyCascade: return "CASCADE";
            case DatabaseMetaData.importedKeySetNull: return "SET NULL";
            case DatabaseMetaData.importedKeySetDefault: return "SET DEFAULT";
            case DatabaseMetaData.importedKeyRestrict: return "RESTRICT";
            case DatabaseMetaData.importedKeyNoAction: return "NO ACTION";
            default: return "UNKNOWN";
        }
    }
}
