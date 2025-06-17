package dev.jhenals.mcp_semgrep_server.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.jhenals.mcp_semgrep_server.models.semgrep_parser.SemgrepFinding;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticAnalysisResult {
    @JsonProperty("findings")
    private List<SemgrepFinding> findings;
    @JsonProperty("errors")
    private List<String> errors;
    @JsonProperty("paths")
    private Map<String, Object> paths;
    @JsonProperty("version")
    private String version;

    public StaticAnalysisResult() {
        this.findings = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.paths = new HashMap<>();
    }
    public boolean hasFindings() { return !findings.isEmpty(); }
    public boolean hasErrors() { return !errors.isEmpty(); }

    public int getFindingCount() { return findings.size(); }
    public int getErrorCount() { return errors.size(); }

}

