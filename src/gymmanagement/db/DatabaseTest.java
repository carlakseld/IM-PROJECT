
package gymmanagement.db;

public class DatabaseTest {

    public static void main(String[] args) {

        System.out.println("Testing SQL Server connection...");

        try {
            Db.testConnection();

            System.out.println("================================");
            System.out.println("DATABASE CONNECTED SUCCESSFULLY!");
            System.out.println("Database: GymManagementDB");
            System.out.println("Server: AKSEL");
            System.out.println("================================");

            Db.close();

        } catch (Exception e) {

            System.out.println("================================");
            System.out.println("DATABASE CONNECTION FAILED!");
            System.out.println("================================");

            System.out.println("Error: " + e.getMessage());

            e.printStackTrace();
        }
    }
}
