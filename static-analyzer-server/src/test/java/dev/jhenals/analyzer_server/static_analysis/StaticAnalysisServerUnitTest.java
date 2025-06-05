package dev.jhenals.static_analyzer_server.static_analysis;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.static_analyzer_server.models.Commit;
import dev.jhenals.static_analyzer_server.models.Issue;
import dev.jhenals.static_analyzer_server.models.PRInput;
import dev.jhenals.static_analyzer_server.models.StaticAnalysisResult;
import dev.jhenals.static_analyzer_server.service.StaticAnalysisService;
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
                public class dummyCode {
                              public static void main(String[] args) {
                                  String password = "admin123";
                                  System.out.println("Password: " + password);
                              }
                          }
        """;

        StaticAnalysisService service = new StaticAnalysisService();
        String result = service.analyzeCode(javaSourceCode);

        System.out.println("SEMGREP JSON OUTPUT:");
        System.out.println(result);

    }
}
