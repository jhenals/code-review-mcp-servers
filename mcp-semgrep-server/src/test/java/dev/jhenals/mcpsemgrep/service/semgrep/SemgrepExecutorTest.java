package dev.jhenals.mcpsemgrep.service.semgrep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.util.ProcessUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SemgrepExecutorTest {

    @Mock
    private ProcessUtils processUtils;

    @Mock
    private SemgrepConfigurationManager configurationManager;

    @InjectMocks
    private SemgrepExecutor semgrepExecutor;

    @Captor
    private ArgumentCaptor<List<String>> commandCaptor;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void executeAnalysis_success() throws Exception {
        String filePath = "/path/to/file.java";
        String config = "p/java";

        String validatedConfig = "p/java";
        when(configurationManager.validateAndNormalizeConfig(config)).thenReturn(validatedConfig);

        JsonNode expectedJson = objectMapper.readTree("{\"results\":[]}");
        ProcessUtils.ProcessExecutionBuilder builderMock = mock(ProcessUtils.ProcessExecutionBuilder.class);

        when(processUtils.builder()).thenReturn(builderMock);
        when(builderMock.command(anyList())).thenReturn(builderMock);
        when(builderMock.timeout(anyInt())).thenReturn(builderMock);
        when(builderMock.expectJson(true)).thenReturn(builderMock);
        when(builderMock.validateCommands(false)).thenReturn(builderMock);
        when(builderMock.executeForJson(processUtils)).thenReturn(expectedJson);

        // Semgrep availability check
        when(processUtils.isCommandAvailable("semgrep")).thenReturn(true);

        JsonNode result = semgrepExecutor.executeAnalysis(filePath, config);

        assertNotNull(result);
        assertEquals(expectedJson, result);

        verify(configurationManager).validateAndNormalizeConfig(config);
        verify(processUtils).isCommandAvailable("semgrep");
        verify(builderMock).command(commandCaptor.capture());

        List<String> command = commandCaptor.getValue();
        assertTrue(command.contains("semgrep"));
        assertTrue(command.contains("--config"));
        assertTrue(command.contains(validatedConfig));
        assertTrue(command.contains(filePath));
    }

    @Test
    void executeAnalysis_semgrepNotAvailable_throwsException() {
        when(processUtils.isCommandAvailable("semgrep")).thenReturn(false);

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> semgrepExecutor.executeAnalysis("/file", "config"));

        assertEquals("SEMGREP_NOT_AVAILABLE", ex.getCode());
        assertTrue(ex.getMessage().contains("Semgrep is not installed"));
    }

    @Test
    void executeAnalysis_processUtilsThrows_throwsMcpAnalysisException() throws Exception {
        String filePath = "/file";
        String config = "p/java";

        when(processUtils.isCommandAvailable("semgrep")).thenReturn(true);
        when(configurationManager.validateAndNormalizeConfig(config)).thenReturn(config);

        ProcessUtils.ProcessExecutionBuilder builderMock = mock(ProcessUtils.ProcessExecutionBuilder.class);
        when(processUtils.builder()).thenReturn(builderMock);
        when(builderMock.command(anyList())).thenReturn(builderMock);
        when(builderMock.timeout(anyInt())).thenReturn(builderMock);
        when(builderMock.expectJson(true)).thenReturn(builderMock);
        when(builderMock.validateCommands(false)).thenReturn(builderMock);
        when(builderMock.executeForJson(processUtils)).thenThrow(new McpAnalysisException("ERR", "fail"));

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> semgrepExecutor.executeAnalysis(filePath, config));

        assertEquals("SEMGREP_ANALYSIS_FAILED", ex.getCode());
        assertTrue(ex.getMessage().contains("Semgrep analysis failed"));
    }

    @Test
    void executeAnalysisWithCustomRules_success() throws Exception {
        String filePath = "/file";
        String ruleFilePath = "/rules.yml";

        when(processUtils.isCommandAvailable("semgrep")).thenReturn(true);

        JsonNode expectedJson = objectMapper.readTree("{\"results\":[]}");
        ProcessUtils.ProcessExecutionBuilder builderMock = mock(ProcessUtils.ProcessExecutionBuilder.class);

        when(processUtils.builder()).thenReturn(builderMock);
        when(builderMock.command(anyList())).thenReturn(builderMock);
        when(builderMock.timeout(anyInt())).thenReturn(builderMock);
        when(builderMock.expectJson(true)).thenReturn(builderMock);
        when(builderMock.executeForJson(processUtils)).thenReturn(expectedJson);

        JsonNode result = semgrepExecutor.executeAnalysisWithCustomRules(filePath, ruleFilePath);

        assertNotNull(result);
        assertEquals(expectedJson, result);

        verify(processUtils).isCommandAvailable("semgrep");
        verify(builderMock).command(commandCaptor.capture());

        List<String> command = commandCaptor.getValue();
        assertTrue(command.contains("semgrep"));
        assertTrue(command.contains("--config"));
        assertTrue(command.contains(ruleFilePath));
        assertTrue(command.contains(filePath));
    }

    @Test
    void executeAnalysisWithCustomRules_processUtilsThrows_throwsMcpAnalysisException() throws Exception {
        String filePath = "/file";
        String ruleFilePath = "/rules.yml";

        when(processUtils.isCommandAvailable("semgrep")).thenReturn(true);

        ProcessUtils.ProcessExecutionBuilder builderMock = mock(ProcessUtils.ProcessExecutionBuilder.class);
        when(processUtils.builder()).thenReturn(builderMock);
        when(builderMock.command(anyList())).thenReturn(builderMock);
        when(builderMock.timeout(anyInt())).thenReturn(builderMock);
        when(builderMock.expectJson(true)).thenReturn(builderMock);
        when(builderMock.executeForJson(processUtils)).thenThrow(new McpAnalysisException("ERR", "fail"));

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> semgrepExecutor.executeAnalysisWithCustomRules(filePath, ruleFilePath));

        assertEquals("SEMGREP_CUSTOM_ANALYSIS_FAILED", ex.getCode());
        assertTrue(ex.getMessage().contains("Semgrep custom rule analysis failed"));
    }

    @Test
    void executeSecurityAnalysis_callsExecuteAnalysisWithSecurityConfig() throws Exception {
        SemgrepExecutor spyExecutor = spy(semgrepExecutor);

        String filePath = "/file";
        JsonNode expectedJson = objectMapper.readTree("{\"results\":[]}");
        doReturn(expectedJson).when(spyExecutor).executeAnalysis(filePath, "p/security");

        JsonNode result = spyExecutor.executeSecurityAnalysis(filePath, "p/security");

        assertEquals(expectedJson, result);
        verify(spyExecutor).executeAnalysis(filePath, "p/security");
    }

    @Test
    void getSemgrepVersion_success() throws Exception {
        List<String> expectedCommand = Arrays.asList("semgrep", "--version");
        String versionOutput = "semgrep 1.15.0";

        when(processUtils.executeCommandForString(expectedCommand, 1)).thenReturn(versionOutput);

        String version = semgrepExecutor.getSemgrepVersion();

        assertEquals("1.15.0", version);
        verify(processUtils).executeCommandForString(expectedCommand, 1);
    }

    @Test
    void getSemgrepVersion_outputWithoutVersion_returnsTrimmedOutput() throws Exception {
        List<String> expectedCommand = Arrays.asList("semgrep", "--version");
        String versionOutput = "semgrep";

        when(processUtils.executeCommandForString(expectedCommand, 1)).thenReturn(versionOutput);

        String version = semgrepExecutor.getSemgrepVersion();

        assertEquals("semgrep", version);
    }

    @Test
    void getSemgrepVersion_throwsException_throwsMcpAnalysisException() throws Exception {
        List<String> expectedCommand = Arrays.asList("semgrep", "--version");

        when(processUtils.executeCommandForString(expectedCommand, 1)).thenThrow(new IOException("fail"));

        McpAnalysisException ex = assertThrows(McpAnalysisException.class, () -> semgrepExecutor.getSemgrepVersion());

        assertEquals("VERSION_CHECK_FAILED", ex.getCode());
        assertTrue(ex.getMessage().contains("Failed to get Semgrep version"));
    }

    @Test
    void validateSemgrepAvailability_commandAvailable_noException() throws McpAnalysisException {
        when(processUtils.isCommandAvailable("semgrep")).thenReturn(true);

        assertDoesNotThrow(() -> semgrepExecutor.validateSemgrepAvailability());

        verify(processUtils).isCommandAvailable("semgrep");
    }

    @Test
    void validateSemgrepAvailability_commandNotAvailable_throwsException() {
        when(processUtils.isCommandAvailable("semgrep")).thenReturn(false);

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> semgrepExecutor.validateSemgrepAvailability());

        assertEquals("SEMGREP_NOT_AVAILABLE", ex.getCode());
    }

    @Test
    void isValidRuleFile_validRule_returnsTrue() throws Exception {
        String ruleFilePath = "/rules.yml";
        List<String> expectedCommand = Arrays.asList("semgrep", "--config", ruleFilePath, "--test");

        when(processUtils.executeCommandForString(expectedCommand, 2)).thenReturn("OK");

        boolean valid = semgrepExecutor.isValidRuleFile(ruleFilePath);

        assertTrue(valid);
        verify(processUtils).executeCommandForString(expectedCommand, 2);
    }

    @Test
    void isValidRuleFile_invalidRule_returnsFalse() throws Exception {
        String ruleFilePath = "/rules.yml";
        List<String> expectedCommand = Arrays.asList("semgrep", "--config", ruleFilePath, "--test");

        when(processUtils.executeCommandForString(expectedCommand, 2)).thenThrow(new IOException("fail"));

        boolean valid = semgrepExecutor.isValidRuleFile(ruleFilePath);

        assertFalse(valid);
        verify(processUtils).executeCommandForString(expectedCommand, 2);
    }
}
