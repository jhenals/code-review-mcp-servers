package dev.jhenals.mcp_semgrep_server.semgrep_service;

import dev.jhenals.mcp_semgrep_server.service.SemgrepService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class SemgrepServiceUnitTest {

    @Test
    public void testSemgrepOnJavaSource() throws Exception {
        String javaSourceCode = """
                public class dummyCode {
                              public static void main(String[] args) {
                                  String password = "admin123";
                                  System.out.println("Password: " + password);
                              }
                          }
        """;

        SemgrepService service = new SemgrepService();
        String result = service.analyzeCode(javaSourceCode);

        System.out.println("SEMGREP JSON OUTPUT:");
        System.out.println(result);

    }
}
