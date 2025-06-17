package dev.jhenals.mcp_semgrep_server.models;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SemgrepToolResult(
        @JsonProperty("success") boolean success,
        @JsonProperty("output") StaticAnalysisResult output,
        @JsonProperty("errorCode") String errorCode,
        @JsonProperty("errorMessage") String errorMessage
) {
    @JsonCreator
    public SemgrepToolResult {
    }

    // Static factory method for a successful Semgrep scan result
    public static SemgrepToolResult scanSuccess(StaticAnalysisResult output) {
        return new SemgrepToolResult(true, output, null, null);
    }

    // Static factory method for a successful security check result
    public static SemgrepToolResult securityCheckSuccess(StaticAnalysisResult securityCheckResult) {
        return new SemgrepToolResult(true, securityCheckResult, null, null);
    }

    // Static factory method for an error result
    public static SemgrepToolResult error(String code, String errorMessage) {
        return new SemgrepToolResult(false, null, code, errorMessage);
    }

}
