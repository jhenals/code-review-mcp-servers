package dev.jhenals.analyzer_server.static_analysis;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.analyzer_server.models.Commit;
import dev.jhenals.analyzer_server.models.Issue;
import dev.jhenals.analyzer_server.models.PRInput;
import dev.jhenals.analyzer_server.models.StaticAnalysisResult;
import dev.jhenals.analyzer_server.service.StaticAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class StaticAnalysisServerUnitTest {

    @Test
    public void testSemgrepOnJavaSource() throws Exception {
        String javaSourceCode = """
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
                
        """;

        StaticAnalysisService service = new StaticAnalysisService();
        List<Issue> result = service.analyzeCode(javaSourceCode);

        System.out.println("✅ SEMGREP JSON OUTPUT:");
        System.out.println(result.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n ")));

    }
}
