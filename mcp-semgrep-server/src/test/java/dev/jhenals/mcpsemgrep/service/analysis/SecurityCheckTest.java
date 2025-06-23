package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.McpSemgrepServerApplication;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.util.FileUtils;
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
@SpringBootTest(classes = McpSemgrepServerApplication.class)
public class SecurityCheckTest {
    /*

    private SecurityAnalysisService securityAnalysisService;

    @BeforeEach
    void setUp() {
        securityAnalysisService = new SecurityAnalysisService();
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

        try (MockedStatic<FileUtils> utilsMock = mockStatic(FileUtils.class)) {

            utilsMock.when(() -> FileUtils.createTemporaryFile(any(CodeFile.class))).thenReturn(fakeFile);
            utilsMock.when(() -> FileUtils.runSemgrepService(any(), anyString())).thenReturn(dummyOutput);
            utilsMock.when(() -> FileUtils.cleanupTempDir(anyString())).thenAnswer(invocation -> null);

            AnalysisResult result = securityAnalysisService.performSecurityCheck(input);

            assertNotNull(result);
            System.out.println(result.toString());
            assertTrue(result.hasFindings());

            assertTrue(result.getFindings().stream().anyMatch(f -> f.getCheckId().equals("hardcoded-password")));
            utilsMock.verify(() -> FileUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
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

        try (MockedStatic<FileUtils> utilsMock = mockStatic(FileUtils.class)) {
            utilsMock.when(() -> FileUtils.createTemporaryFile(any(CodeFile.class))).thenReturn(fakeFile);
            utilsMock.when(() -> FileUtils.runSemgrepService(any(), anyString())).thenReturn(dummyOutput);
            utilsMock.when(() -> FileUtils.cleanupTempDir(anyString())).thenAnswer(invocation -> null);

            AnalysisResult result = securityAnalysisService.performSecurityCheck(input);
            assertNotNull(result);
            utilsMock.verify(() -> FileUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
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

        try (MockedStatic<FileUtils> utilsMock = mockStatic(FileUtils.class)) {
            utilsMock.when(() -> FileUtils.createTemporaryFile(any(CodeFile.class)))
                    .thenThrow(new IOException("Simulated file creation error"));

            IOException thrown = assertThrows(IOException.class, ()->{
                securityAnalysisService.performSecurityCheck(input);
            });

            // Optional: check exception message
            assertTrue(thrown.getMessage().contains("Simulated file creation error"));
            utilsMock.verify(() -> FileUtils.cleanupTempDir(null), times(1));

        }
        System.out.println("securityCheck invocation during an exception is successfully validated during unit test");
    }



     */






}
