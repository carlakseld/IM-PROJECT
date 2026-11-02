package gymmanagement.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the connection to SQL Server using Windows Authentication
 * and provides simple helper methods (query / execute / transaction).
 */
public final class Db {

    // ======================= CONNECTION SETTINGS =======================
    private static final String SERVER   = "AKSEL";
    private static final String INSTANCE = "";      // leave "" for default instance. Example: "SQLEXPRESS"
    private static final String PORT     = "1433";  // used only when INSTANCE is ""
    private static final String DATABASE = "GymManagementDB";
    // ===================================================================

    private static Connection shared;

    private Db() { }

    /** A block of work that runs inside one database transaction. */
    public interface Work {
        void run(Connection c) throws SQLException;
    }

    /** Result of a query: column names + rows. */
    public static final class Table {
        public final String[] columns;
        public final List<Object[]> rows;

        Table(String[] columns, List<Object[]> rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

    private static String url() {
        StringBuilder sb = new StringBuilder("jdbc:sqlserver://").append(SERVER);
        if (INSTANCE.isEmpty()) {
            sb.append(":").append(PORT);
        } else {
            sb.append(";instanceName=").append(INSTANCE);
        }
        sb.append(";databaseName=").append(DATABASE);
        // Ginamit ang integratedSecurity=true para sa Windows Authentication
        sb.append(";integratedSecurity=true;encrypt=true;trustServerCertificate=true;loginTimeout=10");
        return sb.toString();
    }

    private static synchronized Connection conn() throws SQLException {
        if (shared == null || shared.isClosed() || !shared.isValid(2)) {
            // Walang username at password na ipinapasa sa Windows Authentication
            shared = DriverManager.getConnection(url());
        }
        return shared;
    }

    /** Throws IllegalStateException with a readable message when the database cannot be reached. */
    public static void testConnection() {
        try {
            conn();
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public static synchronized void close() {
        try {
            if (shared != null) shared.close();
        } catch (SQLException ignored) {
            // nothing to do
        }
        shared = null;
    }

    // ------------------------------------------------------------------ reading

    public static Table table(String sql, Object... params) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                String[] cols = new String[n];
                for (int i = 0; i < n; i++) cols[i] = md.getColumnLabel(i + 1);
                List<Object[]> rows = new ArrayList<Object[]>();
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) row[i] = norm(rs.getObject(i + 1));
                    rows.add(row);
                }
                return new Table(cols, rows);
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public static List<Object[]> query(String sql, Object... params) {
        return table(sql, params).rows;
    }

    /** Returns the first column of the first row (or null). */
    public static Object scalar(String sql, Object... params) {
        List<Object[]> rows = query(sql, params);
        return rows.isEmpty() ? null : rows.get(0)[0];
    }

    // ------------------------------------------------------------------ writing

    public static int execute(String sql, Object... params) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Runs several statements as ONE transaction. Everything is rolled back if something fails. */
    public static void transaction(Work work) {
        try {
            Connection c = conn();
            c.setAutoCommit(false);
            try {
                work.run(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Executes a statement inside a transaction. Returns the number of affected rows. */
    public static int run(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT inside a transaction and returns the new IDENTITY value. */
    public static int insertId(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("The database did not return the new ID.");
    }

    /** Marks Active memberships whose expiration date already passed as Expired. */
    public static void autoExpireMemberships() {
        execute("UPDATE Membership SET Status = 'Expired' "
                + "WHERE Status = 'Active' AND ExpirationDate < CAST(GETDATE() AS DATE)");
    }

    // ------------------------------------------------------------------ helpers

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, Types.VARCHAR);
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    private static Object norm(Object o) {
        if (o instanceof java.sql.Timestamp || o instanceof java.sql.Date || o instanceof java.sql.Time) {
            return o.toString();
        }
        return o;
    }

    private static IllegalStateException wrap(SQLException e) {
        return new IllegalStateException(friendly(e), e);
    }

    private static String friendly(SQLException e) {
        int code = e.getErrorCode();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String state = e.getSQLState() == null ? "" : e.getSQLState();

        if (code == 547) {
            if (msg.contains("REFERENCE constraint")) {
                return "This record cannot be deleted because other records still use it "
                        + "(for example memberships, transactions, or sessions).";
            }
            if (msg.contains("CHECK constraint")) {
                return "One of the values is not allowed by a database rule "
                        + "(for example a negative price, or an expiration date before the start date).";
            }
            return "The selected related record does not exist.";
        }
        if (code == 2627 || code == 2601) {
            return "Duplicate value: that contact number, email, name, or reference number already exists.";
        }
        if (code == 18456 || code == 4060 || state.startsWith("08") || msg.contains("TCP/IP connection")
                || msg.contains("Login failed") || msg.contains("No suitable driver") || msg.contains("integrated authentication")) {
            return "Cannot connect to SQL Server using Windows Authentication.\n\n"
                    + "Please check:\n"
                    + " 1. SQL Server is running and TCP/IP is enabled in SQL Server Configuration Manager\n"
                    + " 2. The mssql-jdbc_auth .dll file is placed in your project root folder\n"
                    + " 3. The database '" + DATABASE + "' exists (run the SQL scripts first)\n"
                    + " 4. The mssql-jdbc library was added to the project\n\n"
                    + "Details: " + msg;
        }
        return msg;
    }
}