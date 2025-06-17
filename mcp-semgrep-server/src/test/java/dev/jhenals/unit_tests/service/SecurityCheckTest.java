package dev.jhenals.unit_tests.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.SemgrepServerApplication;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.SemgrepToolResult;
import dev.jhenals.mcp_semgrep_server.models.StaticAnalysisResult;
import dev.jhenals.mcp_semgrep_server.service.SecurityCheckService;
import dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@Slf4j
@SpringBootTest(classes = SemgrepServerApplication.class)
public class SecurityCheckTest {

    private SecurityCheckService securityCheckService;

    @BeforeEach
    void setUp() {
        securityCheckService = new SecurityCheckService();
    }


    @Test
    void testSecurityCheckWithFindings() throws Exception {
        Map<String, Object> input = new HashMap<>();
        Map<String, String> codeFileMap = Map.of(
                "filename", "Test.java",
                "content", "public class Test { String password = \"123456\"; }"
        );
        input.put("code_file", codeFileMap);

        File fakeFile = new File("/tmp/fakeTestFile.java");

        String jsonWithFindings = "{ \"results\": [ { \"check_id\": \"hardcoded-password\", \"message\": \"Hardcoded password detected\" } ] }";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode dummyOutput = objectMapper.readTree(jsonWithFindings);

        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {

            utilsMock.when(() -> SemgrepUtils.createTemporaryFile(any(CodeFile.class))).thenReturn(fakeFile);
            utilsMock.when(() -> SemgrepUtils.runSemgrepService(any(), anyString())).thenReturn(dummyOutput);
            utilsMock.when(() -> SemgrepUtils.cleanupTempDir(anyString())).thenAnswer(invocation -> null);

            StaticAnalysisResult result = securityCheckService.securityCheck(input);

            assertNotNull(result);
            assertTrue(result.hasFindings());

            log.info("static analysis result: {}", result.getFindings());
            assertNotNull(result);
            assertTrue(result.getFindings().stream().anyMatch(f -> f.getRuleId().equals("hardcoded-password")));

            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
        }
        log.info("securityCheck invocation with findings is successfully validated during unit test");
    }
    /*

    @Test
    void testSecurityCheckNoFindings() throws Exception {
        // Prepare input map
        Map<String, Object> input = new HashMap<>();
        Map<String, String> codeFileMap = Map.of(
                "filename", "Clean.java",
                "content", "public class Clean { public void safe() {} }"
        );
        input.put("code_file", codeFileMap);

        // Fake temp file
        File fakeFile = new File("/tmp/fakeCleanFile.java");

        // Prepare dummy JSON output with empty results
        String jsonNoFindings = "{ \"results\": [] }";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode dummyOutput = objectMapper.readTree(jsonNoFindings);

        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {
            utilsMock.when(() -> SemgrepUtils.createTemporaryFile(any(CodeFile.class))).thenReturn(fakeFile);
            utilsMock.when(() -> SemgrepUtils.runSemgrepService(any(), anyString())).thenReturn(dummyOutput);
            utilsMock.when(() -> SemgrepUtils.cleanupTempDir(anyString())).thenAnswer(invocation -> null);

            SemgrepToolResult result = securityCheckService.securityCheck(input);

            assertNotNull(result);
            assertTrue(result.success());

            assertNotNull(checkResult);
            String message = checkResult.getSecurityCheckResult().get("message");
            assertEquals("No security issues found in the code!", message);

            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
        }

        log.info("securityCheck invocation with no findings is successfully validated during unit test");
    }

    @Test
    void testSecurityCheckException()  {
        Map<String, Object> input = new HashMap<>();
        Map<String, String> codeFileMap = Map.of(
                "filename", "Error.java",
                "content", "public class Error {}"
        );
        input.put("code_file", codeFileMap);

        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {
            utilsMock.when(() -> SemgrepUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenThrow(new RuntimeException("Simulated failure"));

            SemgrepToolResult result = securityCheckService.securityCheck(input);

            assertNotNull(result);
            assertFalse(result.success());
            assertEquals("INTERNAL_ERROR", result.errorCode());
            assertTrue(result.errorMessage().contains("Simulated failure"));

            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(null), times(1));
        }
        log.info("securityCheck invocation during an exception is successfully validated during unit test");

    }





     */
}
