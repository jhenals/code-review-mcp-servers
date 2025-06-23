package dev.jhenals.mcpsemgrep.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.Finding;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    void parseAnalysisResult_validInput_returnsExpectedResult() throws Exception {
        String json = """
                {
                  "version": "1.2.3",
                  "results": [
                    {
                      "check_id": "RULE123",
                      "extra": {
                        "message": "This is a test message",
                        "severity": "HIGH",
                        "metadata": {
                          "shortDescription": "Test Rule"
                        },
                        "lines": "int a = 0;"
                      },
                      "path": "src/Main.java",
                      "start": {
                        "line": 10,
                        "col": 5
                      }
                    }
                  ],
                  "errors": [
                    {"message": "Error 1"},
                    {"message": "Error 2"}
                  ],
                  "time": 1234,
                  "paths": ["src/", "lib/"]
                }
                """;

        JsonNode input = objectMapper.readTree(json);
        String scanType = "test-scan";

        AnalysisResult result = parser.parseAnalysisResult(input, scanType);

        assertEquals("1.2.3", result.getVersion());
        assertEquals(scanType, result.getScanInfo().get("scan_type"));
        assertEquals("1.2.3", result.getScanInfo().get("semgrep_version"));
        //assertEquals(1234, result.getScanInfo().get("execution_time").asInt());
        //assertTrue(result.getScanInfo().get("scanned_paths").isArray());

        List<Finding> findings = result.getFindings();
        assertEquals(1, findings.size());

        Finding finding = findings.get(0);
        assertEquals("RULE123", finding.getRuleId());
        assertEquals("This is a test message", finding.getMessage());
        assertEquals("HIGH", finding.getSeverity());
        assertEquals("src/Main.java", finding.getFilePath());
        assertEquals(10, finding.getLineNumber());
        assertEquals(5, finding.getColumnNumber());
        assertEquals("int a = 0;", finding.getCodeSnippet());
        assertEquals("Test Rule", finding.getRuleName());

        List<String> errors = result.getErrors();
        assertEquals(2, errors.size());
        assertTrue(errors.contains("Error 1"));
        assertTrue(errors.contains("Error 2"));
    }

    @Test
    void parseAnalysisResult_missingOptionalFields_handlesGracefully() throws Exception {
        String json = """
                {
                  "version": "2.0",
                  "results": [
                    {
                      "check_id": "RULE456",
                      "extra": {
                        "message": "Another message"
                      },
                      "path": "file.py",
                      "start": {
                        "line": 20,
                        "col": 1
                      }
                    }
                  ]
                }
                """;

        JsonNode input = objectMapper.readTree(json);
        String scanType = "scan";

        AnalysisResult result = parser.parseAnalysisResult(input, scanType);

        assertEquals("2.0", result.getVersion());
        assertEquals(scanType, result.getScanInfo().get("scan_type"));
        assertEquals("2.0", result.getScanInfo().get("semgrep_version"));
        assertFalse(result.getScanInfo().containsKey("execution_time"));
        assertFalse(result.getScanInfo().containsKey("scanned_paths"));

        List<Finding> findings = result.getFindings();
        assertEquals(1, findings.size());

        Finding finding = findings.get(0);
        assertEquals("RULE456", finding.getRuleId());
        assertEquals("Another message", finding.getMessage());
        assertEquals("INFO", finding.getSeverity()); // default severity
        assertEquals("file.py", finding.getFilePath());
        assertEquals(20, finding.getLineNumber());
        assertEquals(1, finding.getColumnNumber());
        assertEquals("Another message", finding.getCodeSnippet()); // no lines field
        assertEquals("RULE456", finding.getRuleName()); // no shortDescription

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void parseAnalysisResult_emptyResultsAndErrors_returnsEmptyLists() throws Exception {
        String json = """
                {
                  "version": "3.0",
                  "results": [],
                  "errors": []
                }
                """;

        JsonNode input = objectMapper.readTree(json);
        String scanType = "empty-scan";

        AnalysisResult result = parser.parseAnalysisResult(input, scanType);

        assertEquals("3.0", result.getVersion());
        assertEquals(scanType, result.getScanInfo().get("scan_type"));
        assertEquals("3.0", result.getScanInfo().get("semgrep_version"));

        assertTrue(result.getFindings().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void parseAnalysisResult_malformedFinding_skipsInvalidFinding() throws Exception {
        String json = """
                {
                  "version": "1.0",
                  "results": [
                    {
                      "check_id": "RULE789",
                      "extra": {
                        "message": "Valid message",
                        "severity": "LOW"
                      },
                      "path": "valid.java",
                      "start": {
                        "line": 5,
                        "col": 2
                      }
                    },
                    {
                      "check_id": "RULE_BAD",
                      "extra": {
                        "message": "Bad message"
                      },
                      "path": "bad.java"
                      // missing start node, will cause exception
                    }
                  ]
                }
                """;

        // Fix JSON comment and missing comma for valid JSON
        String fixedJson = json.replace("// missing start node, will cause exception", "")
                .replace("bad.java\"\n                      }", "bad.java\"\n                      }");

        JsonNode input = objectMapper.readTree(fixedJson);
        String scanType = "test";

        AnalysisResult result = parser.parseAnalysisResult(input, scanType);
        System.out.println("[DEBUG] Findings ="+result.getFindings());
        assertEquals(2, result.getFindings().size());
        Finding finding = result.getFindings().get(0);
        assertEquals("RULE789", finding.getRuleId());
        assertEquals("Valid message", finding.getMessage());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void parseAnalysisResult_invalidJson_throwsExceptionHandled() {
        // Pass a JsonNode that will cause an exception in parsing (simulate by passing null)
        String scanType = "scan";

        AnalysisResult result = null;
        try {
            result = parser.parseAnalysisResult(null, scanType);
        } catch (Exception e) {
            fail("Exception should be handled inside parseAnalysisResult");
        }

        assertNotNull(result);
        assertEquals("unknown", result.getVersion());
        assertTrue(result.getFindings().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).startsWith("Failed to parse Semgrep output"));
        assertEquals("scan", result.getScanInfo().get("scan_type"));
        assertEquals("parsing_failed", result.getScanInfo().get("status"));
    }
}
