package dev.jhenals.mcpsemgrep.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisSummary {

    @JsonProperty("total_findings")
    private int totalFindings;

    @JsonProperty("error_count")
    private int errorCount;

    @JsonProperty("warning_count")
    private int warningCount;

    @JsonProperty("info_count")
    private int infoCount;

    @JsonProperty("error_messages")
    private int errorMessages;

    @JsonProperty("has_findings")
    private boolean hasFindings;

    @JsonProperty("has_errors")
    private boolean hasErrors;

    // ========================================
    // Computed Properties
    // ========================================

    /**
     * Gets the highest severity level found.
     */
    public String getHighestSeverity() {
        if (errorCount > 0) return "ERROR";
        if (warningCount > 0) return "WARNING";
        if (infoCount > 0) return "INFO";
        return "NONE";
    }

    /**
     * Gets the total number of high-severity findings (ERROR level).
     */
    public int getHighSeverityCount() {
        return errorCount;
    }

    /**
     * Gets the percentage of findings that are high severity.
     */
    public double getHighSeverityPercentage() {
        if (totalFindings == 0) return 0.0;
        return (double) errorCount / totalFindings * 100.0;
    }

    /**
     * Checks if the analysis passed (no high-severity findings).
     */
    public boolean isPassed() {
        return errorCount == 0;
    }

    /**
     * Gets a risk assessment based on findings.
     */
    public String getRiskLevel() {
        if (errorCount > 0) return "HIGH";
        if (warningCount > 3) return "MEDIUM";
        if (warningCount > 0 || infoCount > 5) return "LOW";
        return "MINIMAL";
    }

    /**
     * Creates a readable summary string.
     */
    public String getSummaryText() {
        if (totalFindings == 0) {
            return "No issues found";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(totalFindings).append(" issue(s) found");

        if (errorCount > 0) {
            sb.append(" (").append(errorCount).append(" critical");
        }
        if (warningCount > 0) {
            if (errorCount > 0) sb.append(", ");
            else sb.append(" (");
            sb.append(warningCount).append(" warning");
        }
        if (infoCount > 0) {
            if (errorCount > 0 || warningCount > 0) sb.append(", ");
            else sb.append(" (");
            sb.append(infoCount).append(" info");
        }

        if (errorCount > 0 || warningCount > 0 || infoCount > 0) {
            sb.append(")");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getSummaryText();
    }
}
