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

    private static final String SECURITY_CONFIG = "p/security";

    public SecurityAnalysisService(SemgrepExecutor semgrepExecutor, SemgrepResultParser resultParser, FileUtils fileUtils) {
        this.semgrepExecutor = semgrepExecutor;
        this.resultParser = resultParser;
        this.fileUtils = fileUtils;
    }

    public AnalysisResult performSecurityAnalysis(CodeAnalysisRequest request) throws McpAnalysisException, IOException {

        File tempFile = null;
        try {
            tempFile = fileUtils.createTemporaryFile(request.getCodeFile());

            JsonNode rawResult = semgrepExecutor.executeSecurityAnalysis(tempFile.getAbsolutePath());
            resultParser.validateSemgrepOutput(rawResult);
            AnalysisResult result = resultParser.parseAnalysisResult(rawResult);
            logSecurityAnalysisResults(result, request.getCodeFile().getFileName());

            return result;

        } catch (McpAnalysisException e) {
            throw new McpAnalysisException("SECURITY_ANALYSIS_FAILED",
                    "Security analysis failed: " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("I/O error during security analysis: " + e.getMessage());
        } finally {
            fileUtils.cleanupTempFile(tempFile);
        }
    }


    private void logSecurityAnalysisResults(AnalysisResult result, String fileName) {

        if (result.hasFindings()) {
            long securityFindings = result.getSecurityFindings().size();
            long totalFindings = result.getFindingCount();
            double securityPercentage = (double) securityFindings / totalFindings * 100;

            log.info("Security findings: {} of {} total ({}%)",
                    securityFindings, totalFindings, String.format("%.1f", securityPercentage));

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
