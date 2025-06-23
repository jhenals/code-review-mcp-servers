
package dev.jhenals.mcpsemgrep.model.response;

import dev.jhenals.mcpsemgrep.model.domain.Finding;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Complete analysis result containing findings, errors, and metadata.
 * Main response object for all analysis operations.
 */
@Data
@Builder
public class AnalysisResult {

    @JsonProperty("version")
    private final String version;

    @JsonProperty("findings")
    @NonNull
    private final List<Finding> findings;

    @JsonProperty("errors")
    @NonNull
    private final List<String> errors;

    @JsonProperty("summary")
    @NonNull
    private final AnalysisSummary summary;

    @JsonProperty("metadata")
    private final Map<String, Object> metadata;

    @JsonProperty("analysis_timestamp")
    @Builder.Default
    private final LocalDateTime analysisTimestamp = LocalDateTime.now();

    // ========================================
    // Computed Properties
    // ========================================

    /**
     * Gets the total number of findings.
     */
    public int getFindingCount() {
        return findings.size();
    }

    /**
     * Checks if any findings were detected.
     */
    public boolean hasFindings() {
        return getFindingCount() > 0;
    }

    /**
     * Checks if any errors occurred during analysis.
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Gets findings filtered by severity level.
     */
    public List<Finding> getFindingsBySeverity(String severity) {
        return findings.stream()
                .filter(f -> severity.equalsIgnoreCase(f.getSeverity()))
                .collect(Collectors.toList());
    }

    /**
     * Gets high-severity findings (ERROR level).
     */
    public List<Finding> getHighSeverityFindings() {
        return getFindingsBySeverity("ERROR");
    }

    /**
     * Gets security-related findings.
     */
    public List<Finding> getSecurityFindings() {
        return findings.stream()
                .filter(Finding::isSecurityFinding)
                .collect(Collectors.toList());
    }

    /**
     * Gets findings grouped by file.
     */
    public Map<String, List<Finding>> getFindingsByFile() {
        return findings.stream()
                .collect(Collectors.groupingBy(Finding::getFilePath));
    }

    /**
     * Gets findings grouped by severity.
     */
    public Map<String, List<Finding>> getFindingsBySeverityMap() {
        return findings.stream()
                .collect(Collectors.groupingBy(Finding::getSeverity));
    }

    /**
     * Checks if the analysis passed (no high-severity findings).
     */
    public boolean isPassed() {
        return summary != null && summary.isPassed();
    }

    /**
     * Gets the risk level assessment.
     */
    public String getRiskLevel() {
        return summary != null ? summary.getRiskLevel() : "UNKNOWN";
    }

    /**
     * Creates a concise summary for logging or display.
     */
    public String getQuickSummary() {
        if (summary != null) {
            return summary.getSummaryText();
        }
        return String.format("%d findings, %d errors", getFindingCount(),
                errors != null ? errors.size() : 0);
    }
}