package dev.jhenals.mcp_semgrep_server.models.semgrep_parser;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcp_semgrep_server.models.StaticAnalysisResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SemgrepResultParser {

    /**
     * Parse Semgrep JSON output into structured results
     */
    public static StaticAnalysisResult parseSemgrepOutput(JsonNode jsonOutput) {
        StaticAnalysisResult results = new StaticAnalysisResult();

        if (jsonOutput == null) {
            return results;
        }

        // Parse results/findings
        JsonNode resultsNode = jsonOutput.get("results");
        if (resultsNode != null && resultsNode.isArray()) {
            for (JsonNode resultNode : resultsNode) {
                SemgrepFinding finding = parseFinding(resultNode);
                if (finding != null) {
                    results.getFindings().add(finding);
                }
            }
        }

        // Parse errors
        JsonNode errorsNode = jsonOutput.get("errors");
        if (errorsNode != null && errorsNode.isArray()) {
            for (JsonNode errorNode : errorsNode) {
                String errorMessage = getTextValue(errorNode, "message");
                if (errorMessage != null) {
                    results.getErrors().add(errorMessage);
                }
            }
        }

        // Parse paths information
        JsonNode pathsNode = jsonOutput.get("paths");
        if (pathsNode != null) {
            results.setPaths(parseJsonToMap(pathsNode));
        }

        // Parse version
        results.setVersion(getTextValue(jsonOutput, "version"));

        return results;
    }

    /**
     * Parse individual finding from JSON node
     */
    private static SemgrepFinding parseFinding(JsonNode findingNode) {
        if (findingNode == null) return null;

        SemgrepFinding finding = new SemgrepFinding();

        // Basic fields
        finding.setRuleId(getTextValue(findingNode, "check_id"));
        finding.setMessage(getTextValue(findingNode, "message"));
        finding.setSeverity(getTextValue(findingNode, "severity"));
        finding.setFilePath(getTextValue(findingNode, "path"));

        // Parse location information
        JsonNode startNode = findingNode.path("start");
        JsonNode endNode = findingNode.path("end");

        if (!startNode.isMissingNode()) {
            finding.setStartLine(getIntValue(startNode, "line"));
            finding.setStartCol(getIntValue(startNode, "col"));
        }

        if (!endNode.isMissingNode()) {
            finding.setEndLine(getIntValue(endNode, "line"));
            finding.setEndCol(getIntValue(endNode, "col"));
        }

        // Extract matched text if available
        finding.setMatchedText(getTextValue(findingNode, "lines"));

        // Parse extra fields
        JsonNode extraNode = findingNode.get("extra");
        if (extraNode != null) {
            finding.setExtra(parseJsonToMap(extraNode));
        }

        return finding;
    }

    /**
     * Helper method to get text value from JSON node
     */
    private static String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    /**
     * Helper method to get integer value from JSON node
     */
    private static int getIntValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && fieldNode.isInt()) ? fieldNode.asInt() : 0;
    }

    /**
     * Convert JsonNode to Map for flexible access to extra fields
     */
    private static Map<String, Object> parseJsonToMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();
        if (node == null || !node.isObject()) {
            return map;
        }

        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (value.isTextual()) {
                map.put(key, value.asText());
            } else if (value.isNumber()) {
                map.put(key, value.asDouble());
            } else if (value.isBoolean()) {
                map.put(key, value.asBoolean());
            } else if (value.isArray() || value.isObject()) {
                map.put(key, value.toString());
            }
        });

        return map;
    }

    /**
     * Filter findings by severity
     */
    public static List<SemgrepFinding> filterBySeverity(List<SemgrepFinding> findings, String severity) {
        return findings.stream()
                .filter(finding -> severity.equalsIgnoreCase(finding.getSeverity()))
                .collect(Collectors.toList());
    }

    /**
     * Filter findings by rule ID
     */
    public static List<SemgrepFinding> filterByRuleId(List<SemgrepFinding> findings, String ruleId) {
        return findings.stream()
                .filter(finding -> ruleId.equals(finding.getRuleId()))
                .collect(Collectors.toList());
    }

    /**
     * Filter findings by file path pattern
     */
    public static List<SemgrepFinding> filterByFilePath(List<SemgrepFinding> findings, String pathPattern) {
        return findings.stream()
                .filter(finding -> finding.getFilePath() != null &&
                        finding.getFilePath().contains(pathPattern))
                .collect(Collectors.toList());
    }

    /**
     * Group findings by file path
     */
    public static Map<String, List<SemgrepFinding>> groupByFilePath(List<SemgrepFinding> findings) {
        return findings.stream()
                .collect(Collectors.groupingBy(
                        finding -> finding.getFilePath() != null ? finding.getFilePath() : "unknown",
                        Collectors.toList()
                ));
    }

    /**
     * Group findings by severity
     */
    public static Map<String, List<SemgrepFinding>> groupBySeverity(List<SemgrepFinding> findings) {
        return findings.stream()
                .collect(Collectors.groupingBy(
                        finding -> finding.getSeverity() != null ? finding.getSeverity() : "unknown",
                        Collectors.toList()
                ));
    }

    /**
     * Get summary statistics
     */
    public static String getSummary(StaticAnalysisResult results) {
        Map<String, Long> severityCounts = results.getFindings().stream()
                .collect(Collectors.groupingBy(
                        finding -> finding.getSeverity() != null ? finding.getSeverity() : "unknown",
                        Collectors.counting()
                ));

        StringBuilder summary = new StringBuilder();
        summary.append("Semgrep Scan Summary:\n");
        summary.append("Total findings: ").append(results.getFindingCount()).append("\n");
        summary.append("Total errors: ").append(results.getErrorCount()).append("\n");
        summary.append("Severity breakdown:\n");

        severityCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> summary.append("  ")
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n"));

        return summary.toString();
    }

}

