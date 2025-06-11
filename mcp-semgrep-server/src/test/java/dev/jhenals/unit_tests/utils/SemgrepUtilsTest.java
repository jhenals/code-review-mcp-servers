package dev.jhenals.unit_tests.utils;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.SemgrepToolResult;
import dev.jhenals.mcp_semgrep_server.service.StaticAnalysisService;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@Slf4j
@SpringBootTest(classes = SemgrepUtils.class)
public class SemgrepUtilsTest {

    private StaticAnalysisService staticAnalysisService;

    private CodeFile codeFile = new CodeFile();
    private Map<String, Object> input;

    private static final String PARAM_NAME = "config";


    @BeforeEach
    void setUp() {
        staticAnalysisService = spy(new StaticAnalysisService());
        codeFile = new CodeFile("TestFile.java", "public class Test {}");
        input = Map.of(
                "code_file", Map.of("filename", "Test.java", "content", "public class Test {}"),
                "config", "auto"
        );
    }

    @Test
    public void testCreateTemporaryFileWritesContent() throws IOException {
        File tempFile = SemgrepUtils.createTemporaryFile(codeFile);

        assertTrue(tempFile.exists());
        assertEquals("TestFile.java", tempFile.getName());
    }

    @Test
    void testCleanupTempDirCalledOnSuccess() throws Exception {
        File fakeFile = new File("/tmp/fakefile.java");

        var dummyJson = "{\"results\":[], \"errors\":[], \"version\":\"1.0\"}";
        var dummyNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(dummyJson);

        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {

            utilsMock.when( ()->SemgrepUtils.createTemporaryFile(any(CodeFile.class))).thenReturn(fakeFile);
            utilsMock.when( ()->SemgrepUtils.runSemgrepService(any(), anyString())).thenReturn(dummyNode);
            utilsMock.when( ()-> SemgrepUtils.cleanupTempDir(anyString())).thenAnswer(invocation ->null);

            SemgrepToolResult result = staticAnalysisService.semgrepScan(input);
            log.info(result.toString());
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
        }

        log.info("cleanupTempDir invocation during a successful semgrepScan is successfully validated in unit tests");
    }

    @Test
    void testCleanupTempDirCalledOnException() {
        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {

            utilsMock.when( ()->SemgrepUtils.createTemporaryFile(any(CodeFile.class))).thenThrow(new RuntimeException("File creation failed"));
            SemgrepToolResult result = staticAnalysisService.semgrepScan(input);
            log.info(result.toString());
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(null), times(1));
        }
        log.info("cleanupTempDir invocation during an exception in semgrepScan has been successfully verified in unit tests");
    }

    @Test
    public void testRunSemgrepServiceParsesJsonOutput(@TempDir Path tempDir) throws IOException {
        File tempFile = new File(tempDir.toFile(), "Test.java");
    String code = "public class Test { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        Files.writeString(tempFile.toPath(),code);

        ArrayList<String> commands = new ArrayList<>(Arrays.asList("semgrep",
                "--config", "auto",
                "--json",
                "--quiet",
                "--no-git-ignore"));
        commands.add(tempFile.getAbsolutePath());

        JsonNode result = SemgrepUtils.runSemgrepService(commands, tempFile.getAbsolutePath());
        assertNotNull(result);
        System.out.println(result.toPrettyString());
        assertTrue(result.has("results"));
        assertTrue(result.has("version"));
        assertTrue(result.has("errors"));
        assertTrue(result.has("paths"));
        assertTrue(result.has("interfile_languages_used"));
        assertTrue(result.has("skipped_rules"));
        String path = result.get("paths").get("scanned").toString();
        String trimmedPath = path.replaceAll("[\\[\\]\"]", "").replaceAll("\\\\\\\\", "\\\\");
        assertEquals(tempFile.getAbsoluteFile().toString(), trimmedPath);
    }

    @Test
    public void testValidateAbsolutePath_withAbsolutePath( @TempDir Path tempDir) throws Exception {
        Path absPath = tempDir.resolve("file.txt");
        Files.createFile(absPath);
        String result = SemgrepUtils.validateAbsolutePath(absPath.toString(), PARAM_NAME);
        assertEquals(absPath.toRealPath().toString(), result);
    }


    @Test
    public void testValidateAbsolutePath_withRelativePath_throws() {
        String relativePath = "relative/path/to/file.txt";

        McpError ex = assertThrows(McpError.class, () -> SemgrepUtils.validateAbsolutePath(relativePath, PARAM_NAME));
        assertTrue(ex.getMessage().contains("must be an absolute path"));
    }

    @Test
    public void testValidateAbsolutePath_withPathTraversal_throws(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("dir");
        Files.createDirectory(dir);
        Path file = dir.resolve("file.txt");
        Files.createFile(file);

        // Construct a path with traversal that goes outside tempDir
        Path outsidePath = tempDir.resolve("dir").resolve("..").resolve("..").resolve("file.txt").toAbsolutePath();

        // This path is absolute but contains traversal sequences
        McpError ex = assertThrows(McpError.class, () -> SemgrepUtils.validateAbsolutePath(outsidePath.toString(), PARAM_NAME));
        log.info("Exception message: {}", ex.getMessage());
        assertTrue(ex.getCode().contains("INVALID_PARAMS"));
    }


    @Test
    public void testValidateAbsolutePath_withNonExistentPath_throws(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("nonexistentfile.txt");

        McpError ex = assertThrows(McpError.class, () -> SemgrepUtils.validateAbsolutePath(nonExistent.toString(), PARAM_NAME));
        assertTrue(ex.getMessage().contains("Invalid path"));
    }

    @Test
    public void testValidateConfig_withSpecialPrefixes_returnsInput() throws McpError {
        String config = "auto";
        String result = SemgrepUtils.validateConfig(config);
        assertEquals(config, result);


        // Also test "p/something" and "r/something"
        assertEquals("p/something", SemgrepUtils.validateConfig("p/something"));
        assertEquals("r/something", SemgrepUtils.validateConfig("r/something"));
        assertEquals("auto", SemgrepUtils.validateConfig(null));
    }

    @Test
    public void testValidateConfig_withAbsolutePath_callsValidateAbsolutePath(@TempDir Path tempDir) throws Exception {
        Path absPath = tempDir.resolve("file.txt");
        Files.createFile(absPath);

        String result = SemgrepUtils.validateConfig(absPath.toString());
        assertEquals(absPath.toRealPath().toString(), result);
    }

    @Test
    public void testValidateConfig_withRelativePath_throws() {
        String relativePath = "relative/path";

        McpError ex = assertThrows(McpError.class, () -> SemgrepUtils.validateConfig(relativePath));
        assertTrue(ex.getMessage().contains("must be an absolute path"));
    }





}
