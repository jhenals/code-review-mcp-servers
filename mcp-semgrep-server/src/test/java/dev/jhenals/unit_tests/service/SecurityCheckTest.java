package dev.jhenals.unit_tests.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.SemgrepServerApplication;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.StaticAnalysisResult;
import dev.jhenals.mcp_semgrep_server.service.SecurityCheckService;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
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
            System.out.println(result.toString());
            assertTrue(result.hasFindings());

            assertTrue(result.getResults().stream().anyMatch(f -> f.getCheckId().equals("hardcoded-password")));
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
        }
        System.out.println("securityCheck invocation with findings is successfully validated during unit test");
    }

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

            StaticAnalysisResult result = securityCheckService.securityCheck(input);
            assertNotNull(result);
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
        }

        System.out.println("securityCheck invocation with no findings is successfully validated during unit test");
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
                    .thenThrow(new IOException("Simulated file creation error"));

            IOException thrown = assertThrows(IOException.class, ()->{
                securityCheckService.securityCheck(input);
            });

            // Optional: check exception message
            assertTrue(thrown.getMessage().contains("Simulated file creation error"));
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(null), times(1));

        }
        System.out.println("securityCheck invocation during an exception is successfully validated during unit test");
    }








}
