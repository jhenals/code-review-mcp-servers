package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
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
import java.util.*;

@Slf4j
@Service
public class CodeAnalysisService {
    private final SemgrepExecutor semgrepExecutor;
    private final SemgrepResultParser resultParser;
    private final FileUtils fileUtils;

    public CodeAnalysisService(SemgrepExecutor semgrepExecutor,
                               SemgrepResultParser resultParser,
                               FileUtils fileUtils) {
        this.semgrepExecutor = semgrepExecutor;
        this.resultParser = resultParser;
        this.fileUtils = fileUtils;
    }


    public AnalysisResult analyzeCode(CodeAnalysisRequest request)
            throws McpAnalysisException, IOException {

        File tempFile = null;
        try {
            tempFile = fileUtils.createTemporaryFile(request.getCodeFile());

            JsonNode rawResult = semgrepExecutor.executeAnalysis(
                    tempFile.getAbsolutePath(),
                    request.getConfig() != null ? request.getConfig() : "auto"
            );

            AnalysisResult result = resultParser.parseAnalysisResult(rawResult, "basic_scan");

            log.info("Code analysis completed - Found {} findings", result.getFindingCount());
            return result;

        } catch (Exception e) {
            throw new McpAnalysisException("ANALYSIS_FAILED",
                    "Failed to analyze code: " + e.getMessage());
        } finally {
            fileUtils.cleanupTempFile(tempFile);
        }
    }

    public AnalysisResult analyzeCodeWithCustomRules(CodeAnalysisRequest request)
            throws McpAnalysisException, IOException {

        if (request.getCustomRule() == null || request.getCustomRule().trim().isEmpty()) {
            throw new McpAnalysisException("MISSING_CUSTOM_RULE",
                    "Custom rule is required for custom rule analysis");
        }

        File tempCodeFile = null;
        File tempRuleFile = null;
        try {
            tempCodeFile = fileUtils.createTemporaryFile(request.getCodeFile());
            tempRuleFile = fileUtils.createTemporaryRuleFile(request.getCustomRule());

            log.debug("Created temporary files - Code: {}, Rule: {}",
                    tempCodeFile.getAbsolutePath(), tempRuleFile.getAbsolutePath());

            JsonNode rawResult = semgrepExecutor.executeAnalysisWithCustomRules(
                    tempCodeFile.getAbsolutePath(),
                    tempRuleFile.getAbsolutePath()
            );

            AnalysisResult result = resultParser.parseAnalysisResult(rawResult, "custom_rule_scan");

            log.info("Custom rule analysis completed - Found {} findings", result.getFindingCount());
            return result;

        } catch (Exception e) {
            log.error("Error during custom rule analysis for: {}",
                    request.getCodeFile().getFileName(), e);
            throw new McpAnalysisException("CUSTOM_ANALYSIS_FAILED",
                    "Failed to analyze code with custom rules: " + e.getMessage());
        } finally {
            fileUtils.cleanupTempFile(tempCodeFile);
            fileUtils.cleanupTempFile(tempRuleFile);
        }
    }

}
