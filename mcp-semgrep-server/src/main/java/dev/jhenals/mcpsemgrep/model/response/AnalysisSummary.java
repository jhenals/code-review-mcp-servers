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


    public boolean isPassed() {
        return errorCount == 0;
    }

    public String getRiskLevel() {
        if (errorCount > 0) return "HIGH";
        if (warningCount > 3) return "MEDIUM";
        if (warningCount > 0 || infoCount > 5) return "LOW";
        return "MINIMAL";
    }

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
