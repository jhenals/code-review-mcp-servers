package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
public class SecurityAnalysisService {
    private final SemgrepExecutor semgrepExecutor;
    private final SemgrepResultParser resultParser;
    private final FileUtils fileUtils;

    private static final String SECURITY_CONFIG= "p/security";

    public SecurityAnalysisService(SemgrepExecutor semgrepExecutor, SemgrepResultParser resultParser, FileUtils fileUtils) {
        this.semgrepExecutor = semgrepExecutor;
        this.resultParser = resultParser;
        this.fileUtils = fileUtils;
    }

    public AnalysisResult performSecurityAnalysis(CodeAnalysisRequest request) throws McpAnalysisException, IOException {
        log.info("Starting security analysis for: {}", request.getCodeFile().getFileName());

        File tempFile = null;
        try {
            // Step 1: Create temporary file using FileUtils
            tempFile = fileUtils.createTemporaryFile(request.getCodeFile());
            log.debug("Created temporary file for security analysis: {}", tempFile.getAbsolutePath());

            // Step 2: Execute security-focused Semgrep analysis using SemgrepExecutor
            JsonNode rawResult = semgrepExecutor.executeSecurityAnalysis(tempFile.getAbsolutePath());
            log.debug("Semgrep security analysis completed, parsing results...");

            // Step 3: Validate output before parsing
            resultParser.validateSemgrepOutput(rawResult);

            // Step 4: Parse results using instance method (not static)
            AnalysisResult result = resultParser.parseAnalysisResult(rawResult);

            // Step 5: Log security-specific analysis results
            logSecurityAnalysisResults(result, request.getCodeFile().getFileName());

            return result;

        } catch (McpAnalysisException e) {
            log.error("Security analysis failed for: {}", request.getCodeFile().getFileName(), e);
            throw new McpAnalysisException("SECURITY_ANALYSIS_FAILED",
                    "Security analysis failed: " + e.getMessage());
        } catch (IOException e) {
            log.error("I/O error during security analysis for: {}", request.getCodeFile().getFileName(), e);
            throw new IOException("I/O error during security analysis: " + e.getMessage());
        } finally {
            // Step 6: Cleanup using FileUtils (not static method)
            fileUtils.cleanupTempFile(tempFile);
        }
    }
        /*String temporaryFileAbsolutePath= null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));

            temporaryFileAbsolutePath =fileUtils.createTemporaryFile(codeFile).getAbsolutePath();

            //mcpsemgrep --McpConfiguration p/security-code-scan
            ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                    "mcpsemgrep",
                    "--McpConfiguration", "p/security-code-scan",
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            commands.add(temporaryFileAbsolutePath);
            JsonNode output= runSemgrepService(commands, temporaryFileAbsolutePath);
            log.info("Json output: {}", output.toPrettyString());
            return SemgrepResultParser.parseSemgrepOutput(output);
        } catch (McpAnalysisException e) {
            throw new McpAnalysisException("INTERNAL_ERROR", e.getMessage());
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
        }

         */

    private void logSecurityAnalysisResults(AnalysisResult result, String fileName) {
        log.info("Security analysis completed for '{}' - {}", fileName, result.getQuickSummary());

        if (result.hasFindings()) {
            // Log security-specific metrics
            long securityFindings = result.getSecurityFindings().size();
            long totalFindings = result.getFindingCount();
            double securityPercentage = (double) securityFindings / totalFindings * 100;

            log.info("Security findings: {} of {} total ({}%)",
                    securityFindings, totalFindings, String.format("%.1f", securityPercentage));

            // Log high-severity security findings with details
            var criticalSecurityFindings = result.getFindings().stream()
                    .filter(f -> f.isSecurityFinding() && f.isHighSeverity())
                    .collect(java.util.stream.Collectors.toList());

            if (!criticalSecurityFindings.isEmpty()) {
                log.warn("🚨 CRITICAL SECURITY ISSUES DETECTED: {}", criticalSecurityFindings.size());
                criticalSecurityFindings.forEach(finding -> {
                    log.warn("  - {} at line {}: {}",
                            finding.getRuleId(),
                            finding.getLineRange(),
                            finding.getMessage());

                    // Log CWE and OWASP information if available
                    if (!finding.getCweIds().isEmpty()) {
                        log.warn("    CWE: {}", String.join(", ", finding.getCweIds()));
                    }
                    if (!finding.getOwaspCategories().isEmpty()) {
                        log.warn("    OWASP: {}", String.join(", ", finding.getOwaspCategories()));
                    }

                    // Log impact and likelihood
                    if (!"UNKNOWN".equals(finding.getImpact())) {
                        log.warn("    Impact: {}, Likelihood: {}, Confidence: {}",
                                finding.getImpact(), finding.getLikelihood(), finding.getConfidence());
                    }
                });
            }

            // Log risk assessment
            String riskLevel = result.getRiskLevel();
            if ("HIGH".equals(riskLevel)) {
                log.warn("⚠️ HIGH RISK: Immediate security review recommended for '{}'", fileName);
            } else if ("MEDIUM".equals(riskLevel)) {
                log.info("📊 MEDIUM RISK: Security review suggested for '{}'", fileName);
            } else {
                log.info("✅ LOW RISK: Minimal security concerns for '{}'", fileName);
            }

        } else {
            log.info("✅ No security issues detected in '{}'", fileName);
        }

        if (result.hasErrors()) {
            log.warn("Security analysis completed with {} errors:", result.getErrors().size());
            result.getErrors().forEach(error -> log.warn("  Error: {}", error));
        }

        // Log parsing statistics for debugging
        var stats = resultParser.getParsingStatistics(result);
        log.debug("Security analysis parsing statistics: {}", stats);
    }



}
