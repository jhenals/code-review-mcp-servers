package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.model.response.AnalysisSummary;
import dev.jhenals.mcpsemgrep.parser.SemgrepResultParser;
import dev.jhenals.mcpsemgrep.service.semgrep.SemgrepExecutor;
import dev.jhenals.mcpsemgrep.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CodeAnalysisServiceTest {

    @Mock
    private SemgrepExecutor semgrepExecutor;

    @Mock
    private SemgrepResultParser resultParser;

    @Mock
    private FileUtils fileUtils;

    @InjectMocks
    private CodeAnalysisService codeAnalysisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper to create a dummy CodeFile
    private CodeFile createCodeFile() {
        return new CodeFile("Test.java", "public class Test {}");
    }

    // Helper to create a dummy AnalysisResult
    private AnalysisResult createDummyAnalysisResult() {
        AnalysisSummary summary = AnalysisSummary.builder()
                .totalFindings(0)
                .errorCount(0)
                .warningCount(0)
                .infoCount(0)
                .errorMessages(0)
                .hasFindings(false)
                .hasErrors(false)
                .build();

        return AnalysisResult.builder()
                .version("1.0")
                .findings(Collections.emptyList())
                .errors(Collections.emptyList())
                .summary(summary)
                .metadata(Collections.emptyMap())
                .build();
    }


    @Test
    void analyzeCode_success() throws Exception {
        CodeFile codeFile = createCodeFile();
        CodeAnalysisRequest request = CodeAnalysisRequest.forAutoConfig(codeFile);

        File tempFile = mock(File.class);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/Test.java");

        JsonNode dummyJsonNode = mock(JsonNode.class);
        AnalysisResult dummyResult = createDummyAnalysisResult();

        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(semgrepExecutor.executeAnalysis(anyString(), anyString())).thenReturn(dummyJsonNode);
        when(resultParser.parseAnalysisResult(dummyJsonNode)).thenReturn(dummyResult);

        AnalysisResult result = codeAnalysisService.analyzeCode(request);

        assertNotNull(result);
        verify(fileUtils).createTemporaryFile(codeFile);
        verify(semgrepExecutor).executeAnalysis("/tmp/Test.java", "auto");
        verify(resultParser).parseAnalysisResult(dummyJsonNode);
        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void analyzeCode_executorThrowsException_shouldThrowMcpAnalysisException() throws Exception {
        CodeFile codeFile = createCodeFile();
        CodeAnalysisRequest request = CodeAnalysisRequest.forAutoConfig(codeFile);

        File tempFile = mock(File.class);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/Test.java");

        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(semgrepExecutor.executeAnalysis(anyString(), anyString()))
                .thenThrow(new McpAnalysisException("ERROR_CODE", "Executor failed"));

        McpAnalysisException ex = assertThrows(McpAnalysisException.class, () -> {
            codeAnalysisService.analyzeCode(request);
        });

        assertTrue(ex.getMessage().contains("Failed to analyze code"));
        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void analyzeCodeWithCustomRules_success() throws Exception {
        CodeFile codeFile = createCodeFile();
        String customRule = "rules:\n- id: test-rule\n  pattern: $X == $X\n";
        CodeAnalysisRequest request = CodeAnalysisRequest.forCustomRule(codeFile, customRule);

        File tempCodeFile = mock(File.class);
        File tempRuleFile = mock(File.class);

        when(tempCodeFile.getAbsolutePath()).thenReturn("/tmp/Test.java");
        when(tempRuleFile.getAbsolutePath()).thenReturn("/tmp/rule.yaml");

        JsonNode dummyJsonNode = mock(JsonNode.class);
        AnalysisResult dummyResult = createDummyAnalysisResult();

        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempCodeFile);
        when(fileUtils.createTemporaryRuleFile(customRule)).thenReturn(tempRuleFile);
        when(semgrepExecutor.executeAnalysisWithCustomRules("/tmp/Test.java", "/tmp/rule.yaml"))
                .thenReturn(dummyJsonNode);
        when(resultParser.parseAnalysisResult(dummyJsonNode)).thenReturn(dummyResult);

        AnalysisResult result = codeAnalysisService.analyzeCodeWithCustomRules(request);

        assertNotNull(result);
        verify(fileUtils).createTemporaryFile(codeFile);
        verify(fileUtils).createTemporaryRuleFile(customRule);
        verify(semgrepExecutor).executeAnalysisWithCustomRules("/tmp/Test.java", "/tmp/rule.yaml");
        verify(resultParser).parseAnalysisResult(dummyJsonNode);
        verify(fileUtils).cleanupTempFile(tempCodeFile);
        verify(fileUtils).cleanupTempFile(tempRuleFile);
    }

    @Test
    void analyzeCodeWithCustomRules_missingCustomRule_shouldThrowException() {
        CodeFile codeFile = createCodeFile();
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .customRule(null)
                .build();

        McpAnalysisException ex = assertThrows(McpAnalysisException.class, () -> {
            codeAnalysisService.analyzeCodeWithCustomRules(request);
        });

        assertEquals("MISSING_CUSTOM_RULE", ex.getCode());
    }

    @Test
    void analyzeCodeWithCustomRules_executorThrowsException_shouldThrowMcpAnalysisException() throws Exception {
        CodeFile codeFile = createCodeFile();
        String customRule = "rules:\n- id: test-rule\n  pattern: $X == $X\n";
        CodeAnalysisRequest request = CodeAnalysisRequest.forCustomRule(codeFile, customRule);

        File tempCodeFile = mock(File.class);
        File tempRuleFile = mock(File.class);

        when(tempCodeFile.getAbsolutePath()).thenReturn("/tmp/Test.java");
        when(tempRuleFile.getAbsolutePath()).thenReturn("/tmp/rule.yaml");

        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempCodeFile);
        when(fileUtils.createTemporaryRuleFile(customRule)).thenReturn(tempRuleFile);
        when(semgrepExecutor.executeAnalysisWithCustomRules(anyString(), anyString()))
                .thenThrow(new McpAnalysisException("ERROR_CODE", "Executor failed"));

        McpAnalysisException ex = assertThrows(McpAnalysisException.class, () -> {
            codeAnalysisService.analyzeCodeWithCustomRules(request);
        });

        assertTrue(ex.getMessage().contains("Failed to analyze code with custom rules"));
        verify(fileUtils).cleanupTempFile(tempCodeFile);
        verify(fileUtils).cleanupTempFile(tempRuleFile);
    }
}
