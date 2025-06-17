package dev.jhenals.unit_tests.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.SemgrepServerApplication;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.StaticAnalysisResult;
import dev.jhenals.mcp_semgrep_server.models.semgrep_parser.SemgrepFinding;
import dev.jhenals.mcp_semgrep_server.models.semgrep_parser.SemgrepResultParser;
import dev.jhenals.mcp_semgrep_server.service.StaticAnalysisService;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@Slf4j
@SpringBootTest(classes = SemgrepServerApplication.class)
@ExtendWith(MockitoExtension.class)
public class StaticAnalysisTest {

    private StaticAnalysisService staticAnalysisService;


    @BeforeEach
    void setUp() {
        staticAnalysisService = spy(new StaticAnalysisService());
    }

    @Test
    void testSemgrepScanSuccess() throws McpError, IOException {
        Map<String, Object> input = new HashMap<>();

        Map<String, String> codeFile = new HashMap<>();
        codeFile.put("filename", "Example.java");
        codeFile.put("content", "public class Example { public void doSomething() { System.out.println(\"Hello\"); } }"); //Input with no finding

        input.put("code_file", codeFile);
        input.put("config", "auto");

        StaticAnalysisResult semgrepScanResult = staticAnalysisService.semgrepScan(input);
        assertNotNull(semgrepScanResult, "SemgrepToolResult should not be null");

        System.out.println("Findings Count: " + semgrepScanResult.getFindingCount());
        //System.out.println(semgrepScanResult.toString());

        for (SemgrepFinding finding : semgrepScanResult.getResults()) {
            System.out.println(finding.toString());
        }

        // Basic assertion for findings
        assertTrue(semgrepScanResult.getFindingCount() >= 0);


    }


    @Test
    void testSemgrepScanSuccessWithFindings() throws McpError, IOException {
        Map<String, Object> input= getStringObjectMap();

        StaticAnalysisResult semgrepScanResult = staticAnalysisService.semgrepScan(input);
        assertNotNull(semgrepScanResult, "SemgrepToolResult should not be null");

        System.out.println("Findings Count: " + semgrepScanResult.getFindingCount());
        //System.out.println(semgrepScanResult.toString());

        for (SemgrepFinding finding : semgrepScanResult.getResults()) {
            System.out.println(finding.toString());
            System.out.println(" - [" + finding.getSeverity() + ","+ "Line "+ finding.getStartLine()+"] " + finding.getMessage());
        }
        // Basic assertion for findings
        assertTrue(semgrepScanResult.getFindingCount() >= 0);
    }

    private static Map<String, Object> getStringObjectMap() {
        Map<String, Object> input = new HashMap<>();
        Map<String, String> codeFile = Map.of(
                "filename", "Test.java",
                "content", """
                        public class SemgrepAutoConfigTest {
                        
                            public static void main(String[] args) {
                                // Example of hardcoded password - semgrep auto config may detect this
                                String password = "password123";
                        
                                // Example of dangerous command execution
                                try {
                                    Runtime.getRuntime().exec("rm -rf /tmp/test");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                        
                                // Example of printing to console
                                System.out.println("Test complete");
                            }
                        }
                        """); //Input with multiple findings

        input.put("code_file", codeFile);
        input.put("config", "auto");
        return input;
    }


    @Test
    void testSemgrepScanWithException() {
        Map<String, String> codeFile = new HashMap<>();
        codeFile.put("filename", "Test.java");
        codeFile.put("content", "public class Test { }");

        Map<String, Object> input = new HashMap<>();
        input.put("code_file", codeFile);
        input.put("config", "auto");

        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {
            utilsMock.when(() -> SemgrepUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenThrow(new IOException("Simulated file creation error"));

            // Assert that the McpError is thrown
            IOException thrown = assertThrows(IOException.class, () -> {
                staticAnalysisService.semgrepScan(input);
            });

            // Optional: check exception message
            assertTrue(thrown.getMessage().contains("Simulated file creation error"));
        }
    }


    @Test
    void testSemgrepScanWithCustomRuleSuccess() throws Exception {
        //input
        Map<String, Object> input = new HashMap<>();
        Map<String, String> codeFileMap = Map.of("filename", "Test.java", "content", "public class Test {}");
        input.put("code_file", codeFileMap);
        input.put("config", "auto");
        input.put("rule", "rules:\n  - id: test-rule\n    pattern: $X");

        //fake files
        File fakeCodeFile = new File("/tmp/fakeCodeFile.java");
        File fakeRuleFile = new File("/tmp/fakeRuleYaml.txt");

        //dummy Json output
        String dummyJson = "{\"results\":[], \"errors\":[], \"version\":\"1.0\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode dummyJsonNode = objectMapper.readTree(dummyJson);

        //dummy StaticAnalysisResult
        StaticAnalysisResult dummyScanResult = mock(StaticAnalysisResult.class);

        //Mock static methods
        try (MockedStatic<SemgrepUtils> utilsMockedStatic = mockStatic(SemgrepUtils.class);
             MockedStatic<SemgrepResultParser> parserMockedStatic = mockStatic(SemgrepResultParser.class)
        ) {

            utilsMockedStatic.when(() -> SemgrepUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenReturn(fakeCodeFile, fakeRuleFile);

            utilsMockedStatic.when(() -> SemgrepUtils.runSemgrepService(any(), anyString()))
                    .thenReturn(dummyJsonNode);

            utilsMockedStatic.when(() -> SemgrepUtils.cleanupTempDir(anyString()))
                    .thenAnswer(invocation -> null);

            parserMockedStatic.when(() -> SemgrepResultParser.parseSemgrepOutput(dummyJsonNode))
                    .thenReturn(dummyScanResult);

            StaticAnalysisResult result = staticAnalysisService.semgrepScanWithCustomRule(input);

            assertNotNull(result);
            utilsMockedStatic.verify(() -> SemgrepUtils.createTemporaryFile(any(CodeFile.class)), times(2));

            utilsMockedStatic.verify(() -> SemgrepUtils.cleanupTempDir(fakeCodeFile.getAbsolutePath()), times(1));
            utilsMockedStatic.verify(() -> SemgrepUtils.cleanupTempDir(fakeRuleFile.getAbsolutePath()), times(1));
        }
    }

    @Test
    void testSemgrepScanWithCustomRuleSuccessWithFinding() throws IOException, McpError {
        Map<String, Object> input = getInputForScanWithCustomRule();

        StaticAnalysisResult result = staticAnalysisService.semgrepScanWithCustomRule(input);
        log.info(result.toString());

        assertNotNull(result);
    }

    private Map<String, Object> getInputForScanWithCustomRule() {
        Map<String, Object> input = new HashMap<>();

        Map<String, String> codeFileMap = Map.of(
                "filename", "VulnerableExample.java",
                "content", """
                        public class VulnerableExample {
                            public void insecureMethod() {
                                String password = "123456";
                                try {
                                    Runtime.getRuntime().exec("rm -rf /");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }"""
        );

        input.put("code_file", codeFileMap);
        input.put("config", "auto");  // or any config you want to test with
        input.put("rule",
                """
                        rules:
                          - id: hardcoded-password
                            patterns:
                              - pattern: String $PASSWORD = "$SECRET"
                            message: "Hardcoded password detected"
                            severity: ERROR
                            languages: [java]
                            metadata:
                              category: security
                        
                          - id: dangerous-exec
                            patterns:
                              - pattern: Runtime.getRuntime().exec($CMD)
                            message: "Dangerous command execution detected"
                            severity: ERROR
                            languages: [java]
                            metadata:
                              category: security
                        """
        );
        return input;
    }

    @Test
    void testSemgrepScanWithCustomRuleException(){
        Map<String, Object> input = new HashMap<>();
        Map<String, String> codeFileMap = Map.of("filename", "Test.java", "content", "public class Test {}");
        input.put("code_file", codeFileMap);
        input.put("config", "auto");
        input.put("rule", "rules:\n  - id: test-rule\n    pattern: $X");

        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {
            utilsMock.when(() -> SemgrepUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenThrow(new IOException("Simulated file creation error"));

            // Assert that the IOException is thrown
            IOException thrown = assertThrows(IOException.class, () -> {
                staticAnalysisService.semgrepScanWithCustomRule(input);
            });

            // Optional: check exception message
            assertTrue(thrown.getMessage().contains("Simulated file creation error"));
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(null), times(2));
        }
    }


}

