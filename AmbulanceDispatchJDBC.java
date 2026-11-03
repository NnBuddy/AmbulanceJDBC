
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AmbulanceDispatchJDBC {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/AmbulanceDispatchDB";
        String username = "root";
        String password = "changeme";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con = DriverManager.getConnection(
                    url, username, password)) {

                System.out.println("Connected to AmbulanceDispatchDB!");

                DatabaseMetaData dbmd = con.getMetaData();
                List<String> tables = new ArrayList<>();

                try (ResultSet rs = dbmd.getTables(
                        "AmbulanceDispatchDB", null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME"));
                    }
                }

                System.out.println("\nTotal tables: " + tables.size());

                for (String table : tables) {
                    System.out.println("\n==============================================");
                    System.out.println("TABLE: " + table);
                    System.out.println("==============================================");

                    String query = "SELECT * FROM `"
                            + table.replace("`", "``") + "`";

                    try (Statement stmt = con.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {

                        ResultSetMetaData md = rs.getMetaData();
                        int columns = md.getColumnCount();
                        int count = 0;

                        for (int i = 1; i <= columns; i++) {
                            System.out.printf("%-25s",
                                    md.getColumnName(i));
                        }

                        System.out.println();
                        for (int i = 0; i < columns * 25; i++) {
                            System.out.print("-");
                        }
                        System.out.println();

                        while (rs.next()) {
                            for (int i = 1; i <= columns; i++) {
                                String value = rs.getString(i);

                                if (value == null) {
                                    value = "NULL";
                                }

                                System.out.printf("%-25s", value);
                            }

                            System.out.println();
                            count++;
                        }

                        System.out.println("Total records in "
                                + table + ": " + count);
                    }
                }

                System.out.println("\nAll database tables displayed.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
