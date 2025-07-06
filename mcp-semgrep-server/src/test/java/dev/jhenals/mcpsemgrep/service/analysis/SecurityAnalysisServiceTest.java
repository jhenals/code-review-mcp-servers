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

class SecurityAnalysisServiceTest {

    @Mock
    private SemgrepExecutor semgrepExecutor;

    @Mock
    private SemgrepResultParser resultParser;

    @Mock
    private FileUtils fileUtils;

    @InjectMocks
    private SecurityAnalysisService securityAnalysisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void performSecurityAnalysis_success() throws Exception {
        // Arrange
        CodeFile codeFile = mock(CodeFile.class);
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .config("security")
                .build();

        File tempFile = mock(File.class);
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/fakefile");

        JsonNode rawResult = mock(JsonNode.class);
        when(semgrepExecutor.executeSecurityAnalysis("/tmp/fakefile", "security")).thenReturn(rawResult);

        AnalysisResult expectedResult = AnalysisResult.builder().build();
        when(resultParser.parseAnalysisResult(rawResult, "security_scan")).thenReturn(expectedResult);

        // Act
        AnalysisResult actualResult = securityAnalysisService.performSecurityAnalysis(request);

        // Assert
        assertSame(expectedResult, actualResult);
        verify(fileUtils).createTemporaryFile(codeFile);
        verify(semgrepExecutor).executeSecurityAnalysis("/tmp/fakefile", "security");
        verify(resultParser).parseAnalysisResult(rawResult, "security_scan");
        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void performSecurityAnalysis_mcpAnalysisExceptionThrown() throws Exception {
        // Arrange
        CodeFile codeFile = mock(CodeFile.class);
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .config("security")
                .build();

        File tempFile = mock(File.class);
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/fakefile");

        McpAnalysisException causeException = new McpAnalysisException("SOME_CODE", "some message");
        when(semgrepExecutor.executeSecurityAnalysis("/tmp/fakefile", "security")).thenThrow(causeException);

        // Act & Assert
        McpAnalysisException thrown = assertThrows(McpAnalysisException.class, () ->
                securityAnalysisService.performSecurityAnalysis(request));

        assertEquals("SECURITY_ANALYSIS_FAILED", thrown.getCode());
        assertTrue(thrown.getMessage().contains("Security analysis failed"));
        assertTrue(thrown.getMessage().contains("some message"));

        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void performSecurityAnalysis_ioExceptionThrown() throws Exception {
        // Arrange
        CodeFile codeFile = mock(CodeFile.class);
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .config("security")
                .build();

        File tempFile = mock(File.class);
        when(fileUtils.createTemporaryFile(codeFile)).thenReturn(tempFile);
        when(tempFile.getAbsolutePath()).thenReturn("/tmp/fakefile");

        IOException causeException = new IOException("disk error");
        when(semgrepExecutor.executeSecurityAnalysis("/tmp/fakefile", "security")).thenThrow(causeException);

        // Act & Assert
        IOException thrown = assertThrows(IOException.class, () ->
                securityAnalysisService.performSecurityAnalysis(request));

        assertTrue(thrown.getMessage().contains("I/O error during security analysis"));
        assertTrue(thrown.getMessage().contains("disk error"));

        verify(fileUtils).cleanupTempFile(tempFile);
    }

    @Test
    void performSecurityAnalysis_cleanupCalledEvenIfCreateTempFileFails() throws Exception {
        // Arrange
        CodeFile codeFile = mock(CodeFile.class);
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .build();

        when(fileUtils.createTemporaryFile(codeFile)).thenThrow(new IOException("cannot create"));

        // Act & Assert
        IOException thrown = assertThrows(IOException.class, () ->
                securityAnalysisService.performSecurityAnalysis(request));

        assertTrue(thrown.getMessage().contains("cannot create"));

        // cleanupTempFile should be called with null since tempFile was never assigned
        verify(fileUtils).cleanupTempFile(null);
    }
}
