import java.security.Key;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement; // Unused import
import java.util.Base64; // Unused import
import javax.crypto.Cipher;

public class BadPracticeExample {

    private static String dbUrl = "jdbc:mysql://localhost:3306/mydb";
    private static String dbUser = "root";
    private static String dbPassword = "123456"; // Hardcoded password

    public static void main(String[] args) {
        BadPracticeExample obj = new BadPracticeExample();
        obj.doSomething();
        obj.connectDatabase("admin'; DROP TABLE users; --"); // SQL Injection
    }

    public void doSomething() {
        try {
            Cipher cipher = Cipher.getInstance("AES"); // Incomplete cryptographic usage
            cipher.init(Cipher.ENCRYPT_MODE, (Key) null); // Null key (causes exception)
        } catch (Exception e) {
            // Empty catch block
        }

        deprecatedMethod(); // Call to deprecated method
    }

    @Deprecated
    public void deprecatedMethod() {
        System.out.println("This method is deprecated and shouldn't be used.");
    }

    public void connectDatabase(String username) {
        try {
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            Statement stmt = conn.createStatement();
            String query = "SELECT * FROM users WHERE username = '" + username + "'"; // Vulnerable to SQL injection
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                System.out.println("User: " + rs.getString("username"));
            }

        } catch (Exception e) {
            System.out.println("Something went wrong"); // No logging or detailed error
        }
    }
}
