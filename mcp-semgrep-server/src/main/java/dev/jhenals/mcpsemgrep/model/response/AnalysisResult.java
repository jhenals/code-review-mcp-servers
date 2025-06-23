
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

    public int getFindingCount() {
        return findings.size();
    }

    public boolean hasFindings() {
        return getFindingCount() > 0;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }


    public List<Finding> getSecurityFindings() {
        return findings.stream()
                .filter(Finding::isSecurityFinding)
                .collect(Collectors.toList());
    }

    public String getRiskLevel() {
        return summary != null ? summary.getRiskLevel() : "UNKNOWN";
    }

    public String getQuickSummary() {
        if (summary != null) {
            return summary.getSummaryText();
        }
        return String.format("%d findings, %d errors", getFindingCount(),
                errors != null ? errors.size() : 0);
    }
}