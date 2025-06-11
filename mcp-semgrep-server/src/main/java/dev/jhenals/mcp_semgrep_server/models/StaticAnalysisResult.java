package dev.jhenals.mcp_semgrep_server.models;

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
public class StaticAnalysisResult {
    private List<SemgrepFinding> findings;
    private List<String> errors;
    private Map<String, Object> paths;
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

    @Override
    public String toString() {
        return "StaticAnalysisResult{" +
                "findings=" + findings +
                ", errors=" + errors +
                ", paths=" + paths +
                ", version='" + version + '\'' +
                '}';
    }
}

