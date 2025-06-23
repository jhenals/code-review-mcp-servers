package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.McpSemgrepServerApplication;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.parser.SemgrepResultParser;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.util.FileUtils;
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
@SpringBootTest(classes = McpSemgrepServerApplication.class)
@ExtendWith(MockitoExtension.class)
public class StaticAnalysisTest {

    /*
    private CodeAnalysisService codeAnalysisService;


    @BeforeEach
    void setUp() {
        codeAnalysisService = spy(new CodeAnalysisService());
    }

    @Test
    void testSemgrepScanSuccess() throws McpAnalysisException, IOException {
        Map<String, Object> input = new HashMap<>();

        Map<String, String> codeFile = new HashMap<>();
        codeFile.put("filename", "Example.java");
        codeFile.put("content", "public class Example { public void doSomething() { System.out.println(\"Hello\"); } }"); //Input with no finding

        input.put("code_file", codeFile);
        input.put("McpConfiguration", "auto");

        AnalysisResult semgrepScanResult = codeAnalysisService.analyzeCode(input);
        assertNotNull(semgrepScanResult, "SemgrepToolResult should not be null");

        System.out.println("Findings Count: " + semgrepScanResult.getFindingCount());
        System.out.println(semgrepScanResult.toString());

        // Basic assertion for findings
        assertTrue(semgrepScanResult.getFindingCount() >= 0);


    }


    @Test
    void testSemgrepScanSuccessWithFindings() throws McpAnalysisException, IOException {
        Map<String, Object> input= getStringObjectMap();

        AnalysisResult semgrepScanResult = codeAnalysisService.analyzeCode(input);
        assertNotNull(semgrepScanResult, "SemgrepToolResult should not be null");

        System.out.println("Findings Count: " + semgrepScanResult.getFindingCount());
        System.out.println(semgrepScanResult.toString());

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
                                // Example of hardcoded password - semgrep auto McpConfiguration may detect this
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
        input.put("McpConfiguration", "auto");
        return input;
    }


    @Test
    void testSemgrepScanWithException() {
        Map<String, String> codeFile = new HashMap<>();
        codeFile.put("filename", "Test.java");
        codeFile.put("content", "public class Test { }");

        Map<String, Object> input = new HashMap<>();
        input.put("code_file", codeFile);
        input.put("McpConfiguration", "auto");

        try (MockedStatic<FileUtils> utilsMock = mockStatic(FileUtils.class)) {
            utilsMock.when(() -> FileUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenThrow(new IOException("Simulated file creation error"));

            // Assert that the McpAnalysisException is thrown
            IOException thrown = assertThrows(IOException.class, () -> {
                codeAnalysisService.analyzeCode(input);
            });

            // Optional: check exception message
            assertTrue(thrown.getMessage().contains("Simulated file creation error"));
            utilsMock.verify(() -> FileUtils.cleanupTempDir(null), times(1));

        }
    }

    @Test
    void testSemgrepScanWithCustomRuleSuccess() throws Exception {
        //input
        Map<String, Object> input = new HashMap<>();
        Map<String, String> codeFileMap = Map.of("filename", "Test.java", "content", "public class Test {}");
        input.put("code_file", codeFileMap);
        input.put("McpConfiguration", "auto");
        input.put("rule", "rules:\n  - id: test-rule\n    pattern: $X");

        //fake files
        File fakeCodeFile = new File("/tmp/fakeCodeFile.java");
        File fakeRuleFile = new File("/tmp/fakeRuleYaml.txt");

        //dummy Json output
        String dummyJson = "{\"results\":[], \"errors\":[], \"version\":\"1.0\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode dummyJsonNode = objectMapper.readTree(dummyJson);

        //dummy AnalysisResult
        AnalysisResult dummyScanResult = mock(AnalysisResult.class);

        //Mock static methods
        try (MockedStatic<FileUtils> utilsMockedStatic = mockStatic(FileUtils.class);
             MockedStatic<SemgrepResultParser> parserMockedStatic = mockStatic(SemgrepResultParser.class)
        ) {

            utilsMockedStatic.when(() -> FileUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenReturn(fakeCodeFile, fakeRuleFile);

            utilsMockedStatic.when(() -> FileUtils.runSemgrepService(any(), anyString()))
                    .thenReturn(dummyJsonNode);

            utilsMockedStatic.when(() -> FileUtils.cleanupTempDir(anyString()))
                    .thenAnswer(invocation -> null);

            parserMockedStatic.when(() -> SemgrepResultParser.parseSemgrepOutput(dummyJsonNode))
                    .thenReturn(dummyScanResult);

            AnalysisResult result = codeAnalysisService.analyzeCodeWithCustomRules(input);
            System.out.println(result.toString());

            assertNotNull(result);
            utilsMockedStatic.verify(() -> FileUtils.createTemporaryFile(any(CodeFile.class)), times(2));

            utilsMockedStatic.verify(() -> FileUtils.cleanupTempDir(fakeCodeFile.getAbsolutePath()), times(1));
            utilsMockedStatic.verify(() -> FileUtils.cleanupTempDir(fakeRuleFile.getAbsolutePath()), times(1));
        }
    }

    @Test
    void testSemgrepScanWithCustomRuleSuccessWithFinding() throws IOException, McpAnalysisException {
        Map<String, Object> input = getInputForScanWithCustomRule();

        AnalysisResult result = codeAnalysisService.analyzeCodeWithCustomRules(input);
        System.out.println(result.toString());
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
        input.put("McpConfiguration", "auto");  // or any McpConfiguration you want to test with
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
        input.put("McpConfiguration", "auto");
        input.put("rule", "rules:\n  - id: test-rule\n    pattern: $X");

        try (MockedStatic<FileUtils> utilsMock = mockStatic(FileUtils.class)) {
            utilsMock.when(() -> FileUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenThrow(new IOException("Simulated file creation error"));

            // Assert that the IOException is thrown
            IOException thrown = assertThrows(IOException.class, () -> {
                codeAnalysisService.analyzeCodeWithCustomRules(input);
            });

            // Optional: check exception message
            assertTrue(thrown.getMessage().contains("Simulated file creation error"));
            utilsMock.verify(() -> FileUtils.cleanupTempDir(null), times(2));
        }
    }

     */


}

