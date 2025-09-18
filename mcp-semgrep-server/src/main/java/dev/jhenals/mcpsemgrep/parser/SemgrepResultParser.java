package dev.jhenals.mcpsemgrep.parser;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.Finding;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SemgrepResultParser {
    private static final Logger log = LoggerFactory.getLogger(SemgrepResultParser.class);

    public AnalysisResult parseAnalysisResult(JsonNode semgrepOutput, String scanType) throws McpAnalysisException {
        try {
            // Basic information
            String version = extractVersion(semgrepOutput);
            List<Finding> findings = extractSimpleFindings(semgrepOutput);
            List<String> errors = extractSimpleErrors(semgrepOutput);
            Map<String, Object> scanInfo = createScanInfo(scanType, semgrepOutput);

            // Build simple result for LLM analysis
            return AnalysisResult.builder()
                    .version(version)
                    .findings(findings)
                    .errors(errors)
                    .scanInfo(scanInfo)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create simple result: {}", e.getMessage());
            return AnalysisResult.builder()
                    .version("unknown")
                    .findings(new ArrayList<>())
                    .errors(List.of("Failed to parse Semgrep output: " + e.getMessage()))
                    .scanInfo(Map.of("scan_type", scanType, "status", "parsing_failed"))
                    .build();
        }
    }

    private String extractVersion(JsonNode output) {
        return output.path("version").asText("unknown");
    }

    private List<Finding> extractSimpleFindings(JsonNode output) {
        List<Finding> findings = new ArrayList<>();
        JsonNode results = output.path("results");

        if (results.isArray()) {
            for (JsonNode result : results) {
                try {
                    Finding finding = Finding.builder()
                            .ruleId(result.path("check_id").asText())
                            .message(result.path("extra").path("message").asText())
                            .severity(result.path("extra").path("severity").asText("INFO"))
                            .filePath(result.path("path").asText())
                            .lineNumber(result.path("start").path("line").asInt())
                            .columnNumber(result.path("start").path("col").asInt())
                            .codeSnippet(extractCodeSnippet(result))
                            .ruleName(extractRuleName(result))
                            .build();

                    findings.add(finding);
                } catch (Exception e) {
                    log.warn("Failed to parse finding: {}", e.getMessage());
                }
            }
        }

        return findings;
    }

    private String extractCodeSnippet(JsonNode result) {
        JsonNode extra = result.path("extra");
        if (extra.has("lines")) {
            return extra.path("lines").asText();
        }
        return result.path("extra").path("message").asText();
    }

    private String extractRuleName(JsonNode result) {
        JsonNode metadata = result.path("extra").path("metadata");
        if (metadata.has("shortDescription")) {
            return metadata.path("shortDescription").asText();
        }
        return result.path("check_id").asText();
    }

    private List<String> extractSimpleErrors(JsonNode output) {
        List<String> errors = new ArrayList<>();
        JsonNode errorsNode = output.path("errors");

        if (errorsNode.isArray()) {
            for (JsonNode error : errorsNode) {
                String errorMsg = error.path("message").asText();
                if (!errorMsg.isEmpty()) {
                    errors.add(errorMsg);
                }
            }
        }

        return errors;
    }

    private Map<String, Object> createScanInfo(String scanType, JsonNode output) {
        Map<String, Object> scanInfo = new HashMap<>();
        scanInfo.put("scan_type", scanType);
        scanInfo.put("semgrep_version", output.path("version").asText("unknown"));

        // Add any timing information if available
        JsonNode time = output.path("time");
        if (!time.isMissingNode()) {
            scanInfo.put("execution_time", time);
        }

        // Add rule information if available
        JsonNode paths = output.path("paths");
        if (!paths.isMissingNode()) {
            scanInfo.put("scanned_paths", paths);
        }

        return scanInfo;
    }

}