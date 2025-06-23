package dev.jhenals.mcpsemgrep.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.Finding;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.model.response.AnalysisSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SemgrepResultParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisResult parseAnalysisResult(JsonNode jsonOutput) throws McpAnalysisException {
        if (jsonOutput == null) {
            return createEmptyResult("No output received from Semgrep");
        }

        try {
            log.debug("Parsing Semgrep output with {} top-level fields",
                    jsonOutput.size());

            //basic information
            String version = extractVersion(jsonOutput);
            List<Finding> findings = parseFindings(jsonOutput);
            List<String> errors = parseErrors(jsonOutput);
            Map<String, Object> paths = parsePaths(jsonOutput);
            Map<String, Object> metadata = extractMetadata(jsonOutput);

            // Create summary
            AnalysisSummary summary = createSummary(findings, errors);

            AnalysisResult result = AnalysisResult.builder()
                    .version(version)
                    .findings(findings)
                    .errors(errors)
                    .summary(summary)
                    .metadata(metadata)
                    .build();

            return result;

        } catch (Exception e) {
            throw new McpAnalysisException("PARSE_ERROR",
                    "Failed to parse Semgrep output: " + e.getMessage());
        }
    }

    List<Finding> parseFindings(JsonNode jsonOutput) {
        JsonNode resultsNode = jsonOutput.get("results");
        if (resultsNode == null || !resultsNode.isArray()) {
            return new ArrayList<>();
        }

        List<Finding> findings = new ArrayList<>();

        for (JsonNode resultNode : resultsNode) {
            try {
                Finding finding = parseSingleFinding(resultNode);
                if (finding != null) {
                    findings.add(finding);
                }
            } catch (Exception e) {
                log.warn("Failed to parse individual finding, skipping: {}", e.getMessage());
            }
        }

        return findings;
    }

    Finding parseSingleFinding(JsonNode resultNode) {
        if (resultNode == null) {
            return null;
        }

        //basic fields
        String checkId = getTextValue(resultNode, "check_id");
        String filePath = getTextValue(resultNode, "path");
        String message = extractMessage(resultNode);
        String severity = extractSeverity(resultNode);

        //position information
        Map<String, Integer> startPosition = extractPosition(resultNode, "start");
        Map<String, Integer> endPosition = extractPosition(resultNode, "end");

        //metadata
        Map<String, Object> metadata = extractFindingMetadata(resultNode);

        // snippet
        String codeSnippet = getTextValue(resultNode, "lines");

        return Finding.builder()
                .checkId(checkId)
                .filePath(filePath)
                .message(message)
                .severity(severity)
                .startLine(startPosition.getOrDefault("line", 0))
                .startColumn(startPosition.getOrDefault("col", 0))
                .endLine(endPosition.getOrDefault("line", 0))
                .endColumn(endPosition.getOrDefault("col", 0))
                .codeSnippet(codeSnippet)
                .metadata(metadata)
                .build();
    }

    private Map<String, Integer> extractPosition(JsonNode resultNode, String positionType) {
        Map<String, Integer> position = new HashMap<>();
        JsonNode posNode = resultNode.get(positionType);

        if (posNode != null) {
            position.put("line", posNode.path("line").asInt(0));
            position.put("col", posNode.path("col").asInt(0));
            position.put("offset", posNode.path("offset").asInt(0));
        }

        return position;
    }

    String extractMessage(JsonNode resultNode) {

        String message = getTextValue(resultNode, "message");
        if (message != null && !message.isEmpty()) {
            return message;
        }

        // Try message in extra field
        JsonNode extraNode = resultNode.get("extra");
        if (extraNode != null) {
            message = getTextValue(extraNode, "message");
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }

        return "No message provided";
    }

    String extractSeverity(JsonNode resultNode) {
        // Try direct severity field
        String severity = getTextValue(resultNode, "severity");
        if (severity != null && !severity.isEmpty()) {
            return normalizeSeverity(severity);
        }

        // Try severity in extra field
        JsonNode extraNode = resultNode.get("extra");
        if (extraNode != null) {
            severity = getTextValue(extraNode, "severity");
            if (severity != null && !severity.isEmpty()) {
                return normalizeSeverity(severity);
            }
        }

        return "INFO"; // Default severity
    }

    String normalizeSeverity(String severity) {
        if (severity == null) return "INFO";

        switch (severity.toUpperCase()) {
            case "ERROR":
            case "HIGH":
                return "ERROR";
            case "WARNING":
            case "WARN":
            case "MEDIUM":
                return "WARNING";
            case "INFO":
            case "LOW":
                return "INFO";
            default:
                return severity.toUpperCase();
        }
    }

    Map<String, Object> extractFindingMetadata(JsonNode resultNode) {
        Map<String, Object> metadata = new HashMap<>();

        JsonNode extraNode = resultNode.get("extra");
        if (extraNode != null) {
            // Add metadata fields
            addIfPresent(metadata, "impact", extraNode.path("metadata").path("impact"));
            addIfPresent(metadata, "likelihood", extraNode.path("metadata").path("likelihood"));
            addIfPresent(metadata, "confidence", extraNode.path("metadata").path("confidence"));
            addIfPresent(metadata, "category", extraNode.path("metadata").path("category"));
            addIfPresent(metadata, "cwe", extraNode.path("metadata").path("cwe"));
            addIfPresent(metadata, "owasp", extraNode.path("metadata").path("owasp"));
            addIfPresent(metadata, "technology", extraNode.path("metadata").path("technology"));

            addIfPresent(metadata, "fingerprint", extraNode.path("fingerprint"));

            addIfPresent(metadata, "validation_state", extraNode.path("validation_state"));

            JsonNode metavarsNode = extraNode.get("metavars");
            if (metavarsNode != null && !metavarsNode.isEmpty()) {
                metadata.put("metavars", convertToMap(metavarsNode));
            }
        }

        return metadata;
    }

    List<String> parseErrors(JsonNode jsonOutput) {
        List<String> errors = new ArrayList<>();

        JsonNode errorsNode = jsonOutput.get("errors");
        if (errorsNode != null && errorsNode.isArray()) {
            for (JsonNode errorNode : errorsNode) {
                if (errorNode.isTextual()) {
                    errors.add(errorNode.asText());
                } else if (errorNode.isObject()) {
                    String message = getTextValue(errorNode, "message");
                    String path = getTextValue(errorNode, "path");
                    if (message != null) {
                        errors.add(path != null ? path + ": " + message : message);
                    }
                }
            }
        }

        return errors;
    }

    Map<String, Object> parsePaths(JsonNode jsonOutput) {
        Map<String, Object> paths = new HashMap<>();

        JsonNode pathsNode = jsonOutput.get("paths");
        if (pathsNode != null) {
            paths.putAll((Map<? extends String, ?>) convertToMap(pathsNode));
        }

        return paths;
    }

    String extractVersion(JsonNode jsonOutput) {
        return getTextValue(jsonOutput, "version", "unknown");
    }


    Map<String, Object> extractMetadata(JsonNode jsonOutput) {
        Map<String, Object> metadata = new HashMap<>();

        // Add scan metadata
        addIfPresent(metadata, "interfile_languages_used", jsonOutput.path("interfile_languages_used"));
        addIfPresent(metadata, "skipped_rules", jsonOutput.path("skipped_rules"));

        // Add timing information if available
        JsonNode timingNode = jsonOutput.get("time");
        if (timingNode != null) {
            metadata.put("timing", convertToMap(timingNode));
        }

        return metadata;
    }

    AnalysisSummary createSummary(List<Finding> findings, List<String> errors) {
        Map<String, Integer> severityCounts = findings.stream()
                .collect(Collectors.groupingBy(
                        Finding::getSeverity,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));

        return AnalysisSummary.builder()
                .totalFindings(findings.size())
                .errorCount(severityCounts.getOrDefault("ERROR", 0))
                .warningCount(severityCounts.getOrDefault("WARNING", 0))
                .infoCount(severityCounts.getOrDefault("INFO", 0))
                .errorMessages(errors.size())
                .hasFindings(!findings.isEmpty())
                .hasErrors(!errors.isEmpty())
                .build();
    }

    AnalysisResult createEmptyResult(String reason) {
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
                .version("unknown")
                .findings(new ArrayList<>())
                .errors(Collections.singletonList(reason))
                .summary(summary)
                .metadata(new HashMap<>())
                .build();
    }

    // ========================================
    // Utility Methods
    // ========================================


    String getTextValue(JsonNode node, String fieldName) {
        return getTextValue(node, fieldName, null);
    }

    String getTextValue(JsonNode node, String fieldName, String defaultValue) {
        if (node == null) return defaultValue;
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : defaultValue;
    }

    void addIfPresent(Map<String, Object> map, String key, JsonNode node) {
        if (node != null && !node.isNull() && !node.isMissingNode()) {
            if (node.isArray() || node.isObject()) {
                map.put(key, convertToMap(node));
            } else {
                map.put(key, node.asText());
            }
        }
    }

    Object convertToMap(JsonNode node) {
        try {
            return objectMapper.convertValue(node, Object.class);
        } catch (Exception e) {
            return node.toString();
        }
    }

    public void validateSemgrepOutput(JsonNode jsonOutput) throws McpAnalysisException {
        if (jsonOutput == null) {
            throw new McpAnalysisException("INVALID_OUTPUT", "Semgrep output is null");
        }

        if (!jsonOutput.isObject()) {
            throw new McpAnalysisException("INVALID_OUTPUT",
                    "Semgrep output must be a JSON object");
        }

        // Check for required fields
        if (!jsonOutput.has("results") && !jsonOutput.has("errors")) {
            throw new McpAnalysisException("INVALID_OUTPUT",
                    "Semgrep output must contain 'results' or 'errors' field");
        }

        log.debug("Semgrep output validation passed");
    }

    public Map<String, Object> getParsingStatistics(AnalysisResult result) {
        Map<String, Object> stats = new HashMap<>();

        if (result != null) {
            stats.put("total_findings", result.getFindingCount());
            stats.put("has_errors", result.hasErrors());
            stats.put("version", result.getVersion());

            if (result.getSummary() != null) {
                stats.put("error_findings", result.getSummary().getErrorCount());
                stats.put("warning_findings", result.getSummary().getWarningCount());
                stats.put("info_findings", result.getSummary().getInfoCount());
            }
        }

        return stats;
    }
}