
package dev.jhenals.mcpsemgrep.model.response;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.model.domain.Finding;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisResult {

    @JsonProperty("version")
    private String version;

    @JsonProperty("findings")
    @Builder.Default
    private List<Finding> findings = new ArrayList<>();

    @JsonProperty("errors")
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @JsonProperty("scan_info")
    @Builder.Default
    private Map<String, Object> scanInfo = new HashMap<>();

    @JsonProperty("timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Simple helper methods for Claude and MCP integration
    public boolean hasFindings() {
        return findings != null && !findings.isEmpty();
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public int getFindingCount() {
        return findings != null ? findings.size() : 0;
    }

    public long getHighSeverityCount() {
        return findings != null ?
                findings.stream()
                        .filter(f -> "ERROR".equalsIgnoreCase(f.getSeverity()))
                        .count() : 0;
    }

    public long getCriticalSecurityCount() {
        return findings != null ?
                findings.stream()
                        .filter(f -> f.getRuleId() != null &&
                                (f.getRuleId().contains("security") ||
                                        f.getRuleId().contains("sqli") ||
                                        f.getRuleId().contains("xss") ||
                                        f.getRuleId().contains("injection")))
                        .count() : 0;
    }

    // Create simple summary text for logging
    public String getSimpleSummary() {
        return String.format("Analysis completed: %d findings (%d high severity), %d errors",
                getFindingCount(), getHighSeverityCount(), errors.size());
    }

}