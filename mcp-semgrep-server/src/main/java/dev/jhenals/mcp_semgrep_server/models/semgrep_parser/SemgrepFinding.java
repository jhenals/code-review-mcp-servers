package dev.jhenals.mcp_semgrep_server.models.semgrep_parser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemgrepFinding {
    @JsonProperty("checkId")
    private String checkId;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("startLine")
    private int startLine;

    @JsonProperty("endLine")
    private int endLine;

    @JsonProperty("startCol")
    private int startCol;

    @JsonProperty("endCol")
    private int endCol;

    @JsonProperty("message")
    private String message;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("matchedText")
    private String matchedText;

    @JsonProperty("extra")
    private Map<String, Object> extra;

    @Override
    public String toString() {
        return String.format("SemgrepFinding{checkId='%s', severity='%s', file='%s', line=%d, message='%s'}",
                checkId, severity, filePath, startLine, message);
    }
}
