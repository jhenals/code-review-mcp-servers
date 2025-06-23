package dev.jhenals.mcpsemgrep.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.Finding;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.model.response.AnalysisSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SemgrepResultParserTest {

    private SemgrepResultParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        parser = new SemgrepResultParser();
        objectMapper = new ObjectMapper();
    }

    @Test
    void parseAnalysisResult_nullInput_returnsEmptyResult() throws McpAnalysisException {
        AnalysisResult result = parser.parseAnalysisResult(null);
        assertNotNull(result);
        assertEquals("unknown", result.getVersion());
        assertTrue(result.getFindings().isEmpty());
        assertFalse(result.hasFindings());
        assertTrue(result.hasErrors()); //WARN dev.jhenals.mcpsemgrep.parser.SemgrepResultParser -- Received null JSON output from Semgrep [No output received from Semgrep]
        System.out.println(result.getErrors());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("No output received"));
    }

    @Test
    void parseAnalysisResult_validInput_parsesCorrectly() throws Exception {
        String json = """
                {
                  "version": "1.2.3",
                  "results": [
                    {
                      "check_id": "CKV_001",
                      "path": "src/Main.java",
                      "message": "Test message",
                      "severity": "HIGH",
                      "start": {"line": 10, "col": 5},
                      "end": {"line": 10, "col": 15},
                      "lines": "some code snippet",
                      "extra": {
                        "metadata": {
                          "impact": "high",
                          "likelihood": "medium",
                          "confidence": "high",
                          "category": "security",
                          "cwe": "CWE-79",
                          "owasp": "A1",
                          "technology": "java"
                        },
                        "fingerprint": "abc123",
                        "validation_state": "validated",
                        "metavars": {
                          "VAR1": {"value": "val1"}
                        }
                      }
                    }
                  ],
                  "errors": ["error1", {"message": "error2", "path": "file1"}],
                  "paths": {
                    "scanned": ["src/"],
                    "skipped": []
                  },
                  "interfile_languages_used": ["java"],
                  "skipped_rules": ["rule1"],
                  "time": {
                    "total": 1234
                  }
                }
                """;

        JsonNode jsonNode = objectMapper.readTree(json);
        AnalysisResult result = parser.parseAnalysisResult(jsonNode);

        assertEquals("1.2.3", result.getVersion());
        assertEquals(1, result.getFindings().size());

        Finding finding = result.getFindings().get(0);
        assertEquals("CKV_001", finding.getCheckId());
        assertEquals("src/Main.java", finding.getFilePath());
        assertEquals("Test message", finding.getMessage());
        assertEquals("ERROR", finding.getSeverity()); // HIGH normalized to ERROR
        assertEquals(10, finding.getStartLine());
        assertEquals(5, finding.getStartColumn());
        assertEquals(10, finding.getEndLine());
        assertEquals(15, finding.getEndColumn());
        assertEquals("some code snippet", finding.getCodeSnippet());

        Map<String, Object> metadata = finding.getMetadata();
        assertNotNull(metadata);
        assertEquals("high", metadata.get("impact"));
        assertEquals("medium", metadata.get("likelihood"));
        assertEquals("high", metadata.get("confidence"));
        assertEquals("security", metadata.get("category"));
        assertEquals("CWE-79", metadata.get("cwe"));
        assertEquals("A1", metadata.get("owasp"));
        assertEquals("java", metadata.get("technology"));
        assertEquals("abc123", metadata.get("fingerprint"));
        assertEquals("validated", metadata.get("validation_state"));
        assertTrue(metadata.containsKey("metavars"));
        assertTrue(metadata.get("metavars") instanceof Map);

        // Errors parsing
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().contains("error1"));
        assertTrue(result.getErrors().contains("file1: error2"));

        // Paths parsing
        Map<String, Object> metadataMap = result.getMetadata();
        assertNotNull(metadataMap);
        assertTrue(metadataMap.containsKey("interfile_languages_used"));
        assertTrue(metadataMap.containsKey("skipped_rules"));
        assertTrue(metadataMap.containsKey("timing"));

        // Summary
        AnalysisSummary summary = result.getSummary();
        assertNotNull(summary);
        assertEquals(1, summary.getTotalFindings());
        assertEquals(1, summary.getErrorCount());
        assertEquals(0, summary.getWarningCount());
        assertEquals(0, summary.getInfoCount());
        assertEquals(2, summary.getErrorMessages());
        assertTrue(summary.isHasFindings());
        assertTrue(summary.isHasErrors());
    }

    @Test
    void parseFindings_noResults_returnsEmptyList() throws Exception {
        JsonNode jsonNode = objectMapper.readTree("{}");
        List<Finding> findings = parser.parseFindings(jsonNode);
        assertNotNull(findings);
        assertTrue(findings.isEmpty());
    }

    @Test
    void parseSingleFinding_nullInput_returnsNull() {
        Finding finding = parser.parseSingleFinding(null);
        assertNull(finding);
    }

    @Test
    void extractMessage_prefersDirectMessage() throws Exception {
        String json = """
                {
                  "message": "direct message",
                  "extra": {
                    "message": "extra message"
                  }
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        String message = parser.extractMessage(node);
        assertEquals("direct message", message);
    }

    @Test
    void extractMessage_fallsBackToExtraMessage() throws Exception {
        String json = """
                {
                  "extra": {
                    "message": "extra message"
                  }
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        String message = parser.extractMessage(node);
        assertEquals("extra message", message);
    }

    @Test
    void extractMessage_noMessage_returnsDefault() throws Exception {
        String json = "{}";
        JsonNode node = objectMapper.readTree(json);
        String message = parser.extractMessage(node);
        assertEquals("No message provided", message);
    }

    @Test
    void extractSeverity_prefersDirectSeverity() throws Exception {
        String json = """
                {
                  "severity": "warning",
                  "extra": {
                    "severity": "error"
                  }
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        String severity = parser.extractSeverity(node);
        assertEquals("WARNING", severity);
    }

    @Test
    void extractSeverity_fallsBackToExtraSeverity() throws Exception {
        String json = """
                {
                  "extra": {
                    "severity": "error"
                  }
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        String severity = parser.extractSeverity(node);
        assertEquals("ERROR", severity);
    }

    @Test
    void extractSeverity_noSeverity_returnsInfo() throws Exception {
        String json = "{}";
        JsonNode node = objectMapper.readTree(json);
        String severity = parser.extractSeverity(node);
        assertEquals("INFO", severity);
    }

    @Test
    void normalizeSeverity_variousInputs() {
        assertEquals("ERROR", parser.normalizeSeverity("error"));
        assertEquals("ERROR", parser.normalizeSeverity("HIGH"));
        assertEquals("WARNING", parser.normalizeSeverity("warn"));
        assertEquals("WARNING", parser.normalizeSeverity("MEDIUM"));
        assertEquals("INFO", parser.normalizeSeverity("info"));
        assertEquals("INFO", parser.normalizeSeverity("low"));
        assertEquals("UNKNOWN", parser.normalizeSeverity("unknown"));
        assertEquals("INFO", parser.normalizeSeverity(null));
    }

    @Test
    void extractFindingMetadata_withVariousFields() throws Exception {
        String json = """
                {
                  "extra": {
                    "metadata": {
                      "impact": "impactVal",
                      "likelihood": "likelihoodVal",
                      "confidence": "confidenceVal",
                      "category": "categoryVal",
                      "cwe": "cweVal",
                      "owasp": "owaspVal",
                      "technology": "techVal"
                    },
                    "fingerprint": "fpVal",
                    "validation_state": "validated",
                    "metavars": {
                      "var1": {"value": "val1"}
                    }
                  }
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        Map<String, Object> metadata = parser.extractFindingMetadata(node);

        assertEquals("impactVal", metadata.get("impact"));
        assertEquals("likelihoodVal", metadata.get("likelihood"));
        assertEquals("confidenceVal", metadata.get("confidence"));
        assertEquals("categoryVal", metadata.get("category"));
        assertEquals("cweVal", metadata.get("cwe"));
        assertEquals("owaspVal", metadata.get("owasp"));
        assertEquals("techVal", metadata.get("technology"));
        assertEquals("fpVal", metadata.get("fingerprint"));
        assertEquals("validated", metadata.get("validation_state"));
        assertTrue(metadata.containsKey("metavars"));
    }

    @Test
    void parseErrors_variousFormats() throws Exception {
        String json = """
                {
                  "errors": [
                    "simple error",
                    {"message": "structured error", "path": "file1"},
                    {"message": "no path error"}
                  ]
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        List<String> errors = parser.parseErrors(node);

        assertEquals(3, errors.size());
        assertTrue(errors.contains("simple error"));
        assertTrue(errors.contains("file1: structured error"));
        assertTrue(errors.contains("no path error"));
    }

    @Test
    void parsePaths_withData() throws Exception {
        String json = """
                {
                  "paths": {
                    "scanned": ["file1", "file2"],
                    "skipped": ["file3"]
                  }
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        Map<String, Object> paths = parser.parsePaths(node);

        assertTrue(paths.containsKey("scanned"));
        assertTrue(paths.containsKey("skipped"));
        assertEquals(List.of("file1", "file2"), paths.get("scanned"));
        assertEquals(List.of("file3"), paths.get("skipped"));
    }

    @Test
    void extractVersion_presentAndAbsent() throws Exception {
        JsonNode withVersion = objectMapper.readTree("{\"version\":\"v1.0\"}");
        assertEquals("v1.0", parser.extractVersion(withVersion));

        JsonNode withoutVersion = objectMapper.readTree("{}");
        assertEquals("unknown", parser.extractVersion(withoutVersion));
    }

    @Test
    void extractMetadata_withVariousFields() throws Exception {
        String json = """
                {
                  "interfile_languages_used": ["java", "python"],
                  "skipped_rules": ["rule1"],
                  "time": {"total": 1000}
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        Map<String, Object> metadata = parser.extractMetadata(node);

        assertTrue(metadata.containsKey("interfile_languages_used"));
        assertTrue(metadata.containsKey("skipped_rules"));
        assertTrue(metadata.containsKey("timing"));
    }

    @Test
    void createSummary_countsCorrectly() {
        Finding errorFinding = Finding.builder().severity("ERROR").build();
        Finding warningFinding = Finding.builder().severity("WARNING").build();
        Finding infoFinding = Finding.builder().severity("INFO").build();

        List<Finding> findings = List.of(errorFinding, warningFinding, warningFinding, infoFinding, infoFinding, infoFinding);
        List<String> errors = List.of("error1", "error2");

        AnalysisSummary summary = parser.createSummary(findings, errors);

        assertEquals(6, summary.getTotalFindings());
        assertEquals(1, summary.getErrorCount());
        assertEquals(2, summary.getWarningCount());
        assertEquals(3, summary.getInfoCount());
        assertEquals(2, summary.getErrorMessages());
        assertTrue(summary.isHasFindings());
        assertTrue(summary.isHasErrors());
    }

    @Test
    void createEmptyResult_containsReason() {
        AnalysisResult result = parser.createEmptyResult("reason"); //AnalysisResult(version=unknown, findings=[], errors=[reason], summary=No issues found, metadata={}, analysisTimestamp=2025-06-23T10:03:52.386676500)
        System.out.println("[DEBUG]:" + result.toString());
        assertEquals("unknown", result.getVersion());
        assertTrue(result.getFindings().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertEquals("reason", result.getErrors().get(0));
        assertFalse(result.hasFindings());
        assertTrue(result.hasErrors()); //errors=[reason]
    }

    @Test
    void getTextValue_variousCases() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {
                  "field1": "value1",
                  "field2": null
                }
                """);

        assertEquals("value1", parser.getTextValue(node, "field1"));
        assertNull(parser.getTextValue(node, "field2"));
        assertNull(parser.getTextValue(node, "missing"));
        assertEquals("default", parser.getTextValue(node, "missing", "default"));
    }

    @Test
    void addIfPresent_addsCorrectly() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JsonNode node = objectMapper.readTree("""
                {
                  "field1": "value1",
                  "field2": null,
                  "field3": [1,2,3]
                }
                """);

        parser.addIfPresent(map, "field1", node.get("field1"));
        parser.addIfPresent(map, "field2", node.get("field2"));
        parser.addIfPresent(map, "field3", node.get("field3"));
        parser.addIfPresent(map, "missing", null);

        assertEquals("value1", map.get("field1"));
        assertFalse(map.containsKey("field2"));
        assertTrue(map.get("field3") instanceof List);
        assertFalse(map.containsKey("missing"));
    }

    @Test
    void convertToMap_validAndInvalid() throws Exception {
        JsonNode validNode = objectMapper.readTree("""
                {
                  "a": 1,
                  "b": {"c": 2}
                }
                """);
        Object converted = parser.convertToMap(validNode);
        assertInstanceOf(Map.class, converted);

        // For invalid conversion, simulate by passing null (should return "null" string)
        Object invalid = parser.convertToMap(null);
        assertNull(invalid);
    }

    @Test
    void validateSemgrepOutput_validAndInvalid() throws Exception {
        JsonNode validNode = objectMapper.readTree("""
                {
                  "results": []
                }
                """);
        assertDoesNotThrow(() -> parser.validateSemgrepOutput(validNode));

        JsonNode validNode2 = objectMapper.readTree("""
                {
                  "errors": []
                }
                """);
        assertDoesNotThrow(() -> parser.validateSemgrepOutput(validNode2));

        McpAnalysisException ex1 = assertThrows(McpAnalysisException.class,
                () -> parser.validateSemgrepOutput(null));
        assertEquals("INVALID_OUTPUT", ex1.getCode());

        JsonNode notObject = objectMapper.readTree("[]");
        McpAnalysisException ex2 = assertThrows(McpAnalysisException.class,
                () -> parser.validateSemgrepOutput(notObject));
        assertEquals("INVALID_OUTPUT", ex2.getCode());

        JsonNode missingFields = objectMapper.readTree("{}");
        McpAnalysisException ex3 = assertThrows(McpAnalysisException.class,
                () -> parser.validateSemgrepOutput(missingFields));
        assertEquals("INVALID_OUTPUT", ex3.getCode());
    }
//
//    @Test
//    void getParsingStatistics_returnsCorrectStats() {
//        AnalysisSummary summary = AnalysisSummary.builder()
//                .errorCount(1)
//                .warningCount(2)
//                .infoCount(3)
//                .build();
//
//        AnalysisResult result = AnalysisResult.builder()
//                .
//
//        Map<String, Object> stats = parser.getParsingStatistics(result);
//
//        assertEquals(6, stats.get("total_findings"));
//        assertEquals(true, stats.get("has_errors"));
//        assertEquals("v1", stats.get("version"));
//        assertEquals(1, stats.get("error_findings"));
//        assertEquals(2, stats.get("warning_findings"));
//        assertEquals(3, stats.get("info_findings"));
//    }

    @Test
    void getParsingStatistics_nullResult_returnsEmptyMap() {
        Map<String, Object> stats = parser.getParsingStatistics(null);
        assertNotNull(stats);
        assertTrue(stats.isEmpty());
    }
}
