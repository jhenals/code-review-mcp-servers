package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.parser.SemgrepResultParser;
import dev.jhenals.mcpsemgrep.service.semgrep.SemgrepExecutor;
import dev.jhenals.mcpsemgrep.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.File;
import java.io.IOException;

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

    @Test
    void analyzeCode_success() throws Exception {
        CodeFile codeFile = new CodeFile("Test.java","public class Test {}" );
        CodeAnalysisRequest request = CodeAnalysisRequest.forAutoConfig(codeFile);

        File tempFile = mock(File.class);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/Test.java");
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);

        JsonNode rawResult = mock(JsonNode.class);
        when(semgrepExecutor.executeAnalysis("/tmp/Test.java", "auto")).thenReturn(rawResult);

        AnalysisResult expectedResult = AnalysisResult.builder().build();
        when(resultParser.parseAnalysisResult(rawResult, "basic_scan")).thenReturn(expectedResult);

        AnalysisResult actualResult = codeAnalysisService.analyzeCode(request);

        assertSame(expectedResult, actualResult);

        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void analyzeCode_withConfig_success() throws Exception {
        CodeFile codeFile = new CodeFile("Test.java","public class Test {}" );

        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .config("custom-config")
                .build();

        File tempFile = mock(File.class);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/Test.java");
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);

        JsonNode rawResult = mock(JsonNode.class);
        when(semgrepExecutor.executeAnalysis("/tmp/Test.java", "custom-config")).thenReturn(rawResult);

        AnalysisResult expectedResult = AnalysisResult.builder().build();
        when(resultParser.parseAnalysisResult(rawResult, "basic_scan")).thenReturn(expectedResult);

        AnalysisResult actualResult = codeAnalysisService.analyzeCode(request);

        assertSame(expectedResult, actualResult);

        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void analyzeCode_throwsException_wrappedInMcpAnalysisException() throws Exception {
        CodeFile codeFile = new CodeFile("Test.java","public class Test {}" );

        CodeAnalysisRequest request = CodeAnalysisRequest.forAutoConfig(codeFile);

        File tempFile = mock(File.class);
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);

        when(semgrepExecutor.executeAnalysis(anyString(), anyString()))
                .thenThrow(new RuntimeException("semgrep failure"));

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> codeAnalysisService.analyzeCode(request));

        System.out.println("[DEBUG]:"+ex.getMessage());

        assertEquals("ANALYSIS_FAILED", ex.getCode());
        //assertTrue(ex.getMessage().contains("semgrep failure"));

        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void analyzeCodeWithCustomRules_success() throws Exception {
        CodeFile codeFile = new CodeFile("Test.java","public class Test {}" );

        String customRule = "rules:\n- id: test-rule\n  pattern: $X";

        CodeAnalysisRequest request = CodeAnalysisRequest.forCustomRule(codeFile, customRule);

        File tempCodeFile = mock(File.class);
        File tempRuleFile = mock(File.class);

        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempCodeFile);
        when(fileUtils.createTemporaryRuleFile(customRule)).thenReturn(tempRuleFile);

        when(tempCodeFile.getAbsolutePath()).thenReturn("/tmp/codefile.java");
        when(tempRuleFile.getAbsolutePath()).thenReturn("/tmp/rulefile.yaml");

        JsonNode rawResult = mock(JsonNode.class);
        when(semgrepExecutor.executeAnalysisWithCustomRules("/tmp/codefile.java", "/tmp/rulefile.yaml"))
                .thenReturn(rawResult);

        AnalysisResult expectedResult = AnalysisResult.builder().build();
        when(resultParser.parseAnalysisResult(rawResult, "custom_rule_scan")).thenReturn(expectedResult);

        AnalysisResult actualResult = codeAnalysisService.analyzeCodeWithCustomRules(request);

        assertSame(expectedResult, actualResult);

        verify(fileUtils).cleanupTempFile(tempCodeFile);
        verify(fileUtils).cleanupTempFile(tempRuleFile);
    }

    @Test
    void analyzeCodeWithCustomRules_missingCustomRule_throwsException() {
        CodeFile codeFile = new CodeFile("Test.java","public class Test {}" );


        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .customRule("   ") // blank
                .build();

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> codeAnalysisService.analyzeCodeWithCustomRules(request));

        assertEquals("MISSING_CUSTOM_RULE", ex.getCode());
        assertTrue(ex.getMessage().contains("Custom rule is required"));
    }

    @Test
    void analyzeCodeWithCustomRules_throwsException_logsAndThrows() throws Exception {
        CodeFile codeFile = new CodeFile("Test.java","public class Test {}" );


        String customRule = "rules:\n- id: test-rule\n  pattern: $X";

        CodeAnalysisRequest request = CodeAnalysisRequest.forCustomRule(codeFile, customRule);

        File tempCodeFile = mock(File.class);
        File tempRuleFile = mock(File.class);

        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempCodeFile);
        when(fileUtils.createTemporaryRuleFile(customRule)).thenReturn(tempRuleFile);

        when(tempCodeFile.getAbsolutePath()).thenReturn("/tmp/codefile.java");
        when(tempRuleFile.getAbsolutePath()).thenReturn("/tmp/rulefile.yaml");

        when(semgrepExecutor.executeAnalysisWithCustomRules(anyString(), anyString()))
                .thenThrow(new RuntimeException("semgrep custom failure"));

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> codeAnalysisService.analyzeCodeWithCustomRules(request));

        assertEquals("CUSTOM_ANALYSIS_FAILED", ex.getCode());
        assertTrue(ex.getMessage().contains("semgrep custom failure"));

        verify(fileUtils).cleanupTempFile(tempCodeFile);
        verify(fileUtils).cleanupTempFile(tempRuleFile);
    }
}
