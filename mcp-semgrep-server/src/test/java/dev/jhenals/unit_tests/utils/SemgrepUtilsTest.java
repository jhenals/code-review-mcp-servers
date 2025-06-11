package dev.jhenals.unit_tests.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.SemgrepToolResult;
import dev.jhenals.mcp_semgrep_server.service.StaticAnalysisService;
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

    private SemgrepUtils semgrepUtils;
    private StaticAnalysisService staticAnalysisService;
    private CodeFile codeFile= new CodeFile();
    private Map<String, Object> input;

    @BeforeEach
    void setUp() {
        semgrepUtils = spy(new SemgrepUtils());
        staticAnalysisService= spy(new StaticAnalysisService());
        ObjectMapper objectMapper = spy(new ObjectMapper());
        codeFile=  new CodeFile("TestFile.java", "public class Test {}");
        input = Map.of(
                "code_file", Map.of("filename", "Test.java", "content", "public class Test {}"),
                "config", "auto"
        );
    }

    @Test
    public void testCreateTemporaryFileWritesContent(@TempDir Path tempDir) throws IOException {
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
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(fakeFile.getAbsolutePath()), times(1));
        }

        log.info("cleanupTempDir invocation during a successful semgrepScan is successfully validated in unit tests");
    }

    @Test
    void testCleanupTempDirCalledOnException() throws Exception {
        try (MockedStatic<SemgrepUtils> utilsMock = mockStatic(SemgrepUtils.class)) {

            utilsMock.when( ()->SemgrepUtils.createTemporaryFile(any(CodeFile.class))).thenThrow(new RuntimeException("File creation failed"));
            SemgrepToolResult result = staticAnalysisService.semgrepScan(input);
            utilsMock.verify(() -> SemgrepUtils.cleanupTempDir(null), times(1));
        }
        log.info("cleanupTempDir invocation during an exception in semgrepScan has been successfully verified in unit tests");
    }

    @Test
    public void testRunSemgrepServiceParsesJsonOutput(@TempDir Path tempDir) throws IOException {
        File tempFile = new File(tempDir.toFile(), "Test.java");
        String code = "public class Test { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        Files.writeString(tempFile.toPath(), code);

        ArrayList<String> commands = new ArrayList<>(Arrays.asList("semgrep",
                "--config", "auto",
                "--json",
                "--quiet",
                "--no-git-ignore"));
        commands.add(tempFile.getAbsolutePath());

        JsonNode result = semgrepUtils.runSemgrepService(commands, tempFile.getAbsolutePath());
        assertNotNull(result);
        System.out.println(result.toPrettyString());
        assertTrue(result.has("results"));
        assertTrue(result.has("version"));
        assertTrue(result.has("errors"));
        assertTrue(result.has("paths"));
        assertTrue(result.has("interfile_languages_used"));
        assertTrue(result.has("skipped_rules"));
        String path = result.get("paths").get("scanned").toString();
        String trimmedPath = path.replaceAll("[\\[\\]\"]", "").replaceAll("\\\\\\\\", "\\\\");;
        assertEquals(tempFile.getAbsoluteFile().toString(), trimmedPath);
    }

}
