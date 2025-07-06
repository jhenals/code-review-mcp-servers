package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.parser.SemgrepResultParser;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.service.semgrep.SemgrepExecutor;
import dev.jhenals.mcpsemgrep.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class SecurityAnalysisService {
    private final SemgrepExecutor semgrepExecutor;
    private final SemgrepResultParser resultParser;
    private final FileUtils fileUtils;

    public SecurityAnalysisService(SemgrepExecutor semgrepExecutor, SemgrepResultParser resultParser, FileUtils fileUtils) {
        this.semgrepExecutor = semgrepExecutor;
        this.resultParser = resultParser;
        this.fileUtils = fileUtils;
    }

    public AnalysisResult performSecurityAnalysis(CodeAnalysisRequest request) throws McpAnalysisException, IOException {

        File tempFile = null;
        try {
            tempFile = fileUtils.createTemporaryFile(request.getCodeFile());
            String config = request.getConfig() != null ? request.getConfig() : "auto";
            JsonNode rawResult = semgrepExecutor.executeSecurityAnalysis(tempFile.getAbsolutePath(), config);
            return resultParser.parseAnalysisResult(rawResult, "security_scan");
        } catch (McpAnalysisException e) {
            throw new McpAnalysisException("SECURITY_ANALYSIS_FAILED",
                    "Security analysis failed: " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("I/O error during security analysis: " + e.getMessage());
        } finally {
            fileUtils.cleanupTempFile(tempFile);
        }
    }


}
