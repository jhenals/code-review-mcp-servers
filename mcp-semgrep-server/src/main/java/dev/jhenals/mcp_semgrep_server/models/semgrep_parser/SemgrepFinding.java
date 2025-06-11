package dev.jhenals.mcp_semgrep_server.models.semgrep_parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SemgrepFinding {
    private String ruleId;
    private String message;
    private String severity;
    private String filePath;
    private int startLine;
    private int endLine;
    private int startCol;
    private int endCol;
    private String matchedText;
    private Map<String, Object> extra;

    @Override
    public String toString() {
        return String.format("SemgrepFinding{ruleId='%s', severity='%s', file='%s', line=%d, message='%s'}",
                ruleId, severity, filePath, startLine, message);
    }
}
