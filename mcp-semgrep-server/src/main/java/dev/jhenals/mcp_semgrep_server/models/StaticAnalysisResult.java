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
    @JsonProperty("version")
    private String version;
    @JsonProperty("results")
    private List<SemgrepFinding> results;
    @JsonProperty("errors")
    private List<String> errors;
    @JsonProperty("paths")
    private Map<String, Object> paths;


    public StaticAnalysisResult() {
        this.results = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.paths = new HashMap<>();
    }
    public boolean hasFindings() { return !results.isEmpty(); }
    public boolean hasErrors() { return !errors.isEmpty(); }

    public int getFindingCount() { return results.size(); }
    public int getErrorCount() { return errors.size(); }

    public String toString(){
        StringBuilder sb= new StringBuilder();
        sb.append("Version: ").append(version).append("\n");
        sb.append("RESULTS: \n");
        if(!results.isEmpty()){
            for(SemgrepFinding finding: results){
                sb.append("- ").append(finding.toString()).append("\n");
            }
        }else{
            sb.append("- null \n");
        }
        sb.append("ERRORS: \n");
        if(!errors.isEmpty()){
            for(String error: errors){
                sb.append("- ").append(error).append("\n");
            }
        }else{
            sb.append("- null \n");
        }
        sb.append("PATH: ").append(paths);
        return sb.toString();
    }
}

