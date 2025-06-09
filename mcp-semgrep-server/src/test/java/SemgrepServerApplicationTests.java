import dev.jhenals.mcp_semgrep_server.SemgrepServerApplication;
import dev.jhenals.mcp_semgrep_server.models.SemgrepResult;
import dev.jhenals.mcp_semgrep_server.models.SemgrepScanResult;
import dev.jhenals.mcp_semgrep_server.service.SemgrepService;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SemgrepServerApplication.class)
@ExtendWith(MockitoExtension.class)
class SemgrepServerApplicationTests {


    private SemgrepService semgrepService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        semgrepService = new SemgrepService();
        objectMapper = mock(ObjectMapper.class);
    }

    //semgrepScan-------------------------------------------------------------------------------------------------------
    @Test
    void testSemgrepScanSuccess() throws Exception {

        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> codeFiles = new ArrayList<>();
        codeFiles.add(Map.of("filename", "Test.java", "content", "public class Test {}"));
        input.put("code_files", codeFiles);
        input.put("config", "auto");

        String tempDir = "/tmp/semgrep_test_dir";
        List<String> args = List.of("semgrep", "--config", "auto", "--json", "--quiet", "--no-git-ignore", tempDir);
        String semgrepOutput = "{\"results\":[]}";

        SemgrepScanResult dummyScanResult = new SemgrepScanResult();

        try (MockedStatic<SemgrepUtils> utilsMock = Mockito.mockStatic(SemgrepUtils.class);
             MockedStatic<ObjectMapper> objectMapperMock = Mockito.mockStatic(ObjectMapper.class)) {

            utilsMock.when(() -> SemgrepUtils.validateConfig("auto")).thenReturn("auto");
            utilsMock.when(() -> SemgrepUtils.createTempFilesFromCodeContent(anyList())).thenReturn(tempDir);
            utilsMock.when(() -> SemgrepUtils.getSemgrepScanArgs(tempDir, "auto")).thenReturn(args);
            utilsMock.when(() -> SemgrepUtils.runSemgrepDefault(tempDir)).thenReturn(semgrepOutput);
            utilsMock.when(() -> SemgrepUtils.cleanupTempDir(tempDir)).thenAnswer(invocation -> null);

            SemgrepResult result = semgrepService.semgrepScan(input);
            //System.out.println(result);
            assertNotNull(result);

            assertTrue(result.isSuccess());
            assertNotNull(result.getOutput());
            assertNull(result.getSecurityCheckResult());
            assertNull(result.getErrorCode());
            assertNull(result.getErrorMessage());

            assertEquals(dummyScanResult.getResults(), result.getOutput().getResults());
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(tempDir));

        }
    }

    @Test
    void testSemgrepScanMcpError() throws Exception {
        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> codeFiles = new ArrayList<>();
        codeFiles.add(Map.of("filename", "Test.java", "content", "public class Test {}"));
        input.put("code_files", codeFiles);
        input.put("config", "auto");

        try (MockedStatic<SemgrepUtils> utilsMock = Mockito.mockStatic(SemgrepUtils.class)) {
            utilsMock.when(() -> SemgrepUtils.validateConfig("auto")).thenThrow(new McpError("INVALID_PARAMS", "Invalid config"));

            SemgrepResult result = semgrepService.semgrepScan(input);
            System.out.println(result);
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertNull(result.getOutput());
            assertNull(result.getSecurityCheckResult());

            assertNotNull(result.getErrorCode());
            assertNotNull(result.getErrorMessage());
            assertEquals("INVALID_PARAMS", result.getErrorCode());
            assertEquals("Invalid config", result.getErrorMessage());

            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(null));
        }
    }

    @Test
    void testSemgrepScanGenericException() throws Exception {
        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> codeFiles = new ArrayList<>();
        codeFiles.add(Map.of("filename", "Test.java", "content", "public class Test {}"));
        input.put("code_files", codeFiles);
        input.put("config", "auto");

        try (MockedStatic<SemgrepUtils> utilsMock = Mockito.mockStatic(SemgrepUtils.class)) {
            utilsMock.when(() -> SemgrepUtils.validateConfig("auto")).thenReturn("auto");
            utilsMock.when(() -> SemgrepUtils.createTempFilesFromCodeContent(anyList())).thenReturn("/tmp/dir");
            utilsMock.when(() -> SemgrepUtils.getSemgrepScanArgs(anyString(), anyString())).thenReturn(List.of());
            utilsMock.when(() -> SemgrepUtils.runSemgrepDefault(anyString())).thenThrow(new RuntimeException("Unexpected error"));

            SemgrepResult result = semgrepService.semgrepScan(input);
            System.out.println(result);
            assertNotNull(result);

            assertEquals("INTERNAL_ERROR", result.getErrorCode());
            assertTrue(result.getErrorMessage().contains("Unexpected error"));


            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir("/tmp/dir"));
        }
    }

    //semgrepScanWithCustomRule-------------------------------------------------------------------------------------------------------




    //securityCheck-------------------------------------------------------------------------------------------------------



    //getSupportedLanguage-------------------------------------------------------------------------------------------------------


    //getAbstractSyntaxTree-------------------------------------------------------------------------------------------------------


    //getSemgrepRuleSchema-------------------------------------------------------------------------------------------------------
}
