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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SecurityAnalysisServiceTest {

    @Mock
    private SemgrepExecutor semgrepExecutor;

    @Mock
    private SemgrepResultParser resultParser;

    @Mock
    private FileUtils fileUtils;

    @InjectMocks
    private SecurityAnalysisService securityAnalysisService;

    private CodeAnalysisRequest request;
    private CodeFile codeFile;
    private File tempFile;
    private JsonNode rawResult;
    private AnalysisResult analysisResult;

    @BeforeEach
    void setUp() {
        codeFile = new CodeFile("TestFile.java", "public class Test {}");
        request = CodeAnalysisRequest.forAutoConfig(codeFile);

        tempFile = mock(File.class);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/TestFile.java");

        rawResult = mock(JsonNode.class);
        analysisResult = mock(AnalysisResult.class);
    }

    @Test
    void performSecurityAnalysis_success() throws Exception {
        // Arrange
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(semgrepExecutor.executeSecurityAnalysis(tempFile.getAbsolutePath())).thenReturn(rawResult);
        doNothing().when(resultParser).validateSemgrepOutput(rawResult);
        when(resultParser.parseAnalysisResult(rawResult)).thenReturn(analysisResult);

        when(analysisResult.getQuickSummary()).thenReturn("Summary");
        when(analysisResult.hasFindings()).thenReturn(false);
        when(analysisResult.hasErrors()).thenReturn(false);

        // Stub the call inside logSecurityAnalysisResults
        when(resultParser.getParsingStatistics(analysisResult)).thenReturn(Map.of("total_findings", 0));

        // Act
        AnalysisResult result = securityAnalysisService.performSecurityAnalysis(request);

        // Assert
        assertSame(analysisResult, result);

        InOrder inOrder = inOrder(fileUtils, semgrepExecutor, resultParser);
        inOrder.verify(fileUtils).createTemporaryFile(codeFile);
        inOrder.verify(semgrepExecutor).executeSecurityAnalysis(tempFile.getAbsolutePath());
        inOrder.verify(resultParser).validateSemgrepOutput(rawResult);
        inOrder.verify(resultParser).parseAnalysisResult(rawResult);
        inOrder.verify(resultParser).getParsingStatistics(analysisResult);  // verify this call as well
        inOrder.verify(fileUtils).cleanupTempFile(tempFile);

        verifyNoMoreInteractions(fileUtils, semgrepExecutor, resultParser);
    }


    @Test
    void performSecurityAnalysis_mcpAnalysisException() throws Exception {
        // Arrange
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(semgrepExecutor.executeSecurityAnalysis(tempFile.getAbsolutePath()))
                .thenThrow(new McpAnalysisException("CODE", "message"));

        // Act & Assert
        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> securityAnalysisService.performSecurityAnalysis(request));
        assertEquals("SECURITY_ANALYSIS_FAILED", ex.getCode());
        assertTrue(ex.getMessage().contains("Security analysis failed"));

        verify(fileUtils).createTemporaryFile(codeFile);
        verify(semgrepExecutor).executeSecurityAnalysis(tempFile.getAbsolutePath());
        verify(fileUtils).cleanupTempFile(tempFile);
        verifyNoMoreInteractions(fileUtils, semgrepExecutor, resultParser);
    }

    @Test
    void performSecurityAnalysis_ioException() throws Exception {
        // Arrange
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(semgrepExecutor.executeSecurityAnalysis(tempFile.getAbsolutePath()))
                .thenThrow(new IOException("io error"));

        // Act & Assert
        IOException ex = assertThrows(IOException.class,
                () -> securityAnalysisService.performSecurityAnalysis(request));
        assertTrue(ex.getMessage().contains("I/O error during security analysis"));

        verify(fileUtils).createTemporaryFile(codeFile);
        verify(semgrepExecutor).executeSecurityAnalysis(tempFile.getAbsolutePath());
        verify(fileUtils).cleanupTempFile(tempFile);
        verifyNoMoreInteractions(fileUtils, semgrepExecutor, resultParser);
    }

    @Test
    void performSecurityAnalysis_cleanupCalledEvenIfException() throws Exception {
        // Arrange
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(semgrepExecutor.executeSecurityAnalysis(tempFile.getAbsolutePath())).thenReturn(rawResult);
        doThrow(new McpAnalysisException("CODE", "fail")).when(resultParser).validateSemgrepOutput(rawResult);

        // Act & Assert
        assertThrows(McpAnalysisException.class,
                () -> securityAnalysisService.performSecurityAnalysis(request));

        verify(fileUtils).createTemporaryFile(codeFile);
        verify(semgrepExecutor).executeSecurityAnalysis(tempFile.getAbsolutePath());
        verify(resultParser).validateSemgrepOutput(rawResult);
        verify(fileUtils).cleanupTempFile(tempFile);
    }
}
