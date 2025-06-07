package dev.jhenals.mcp_semgrep_server.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class Issue{
    private String issueType;
    private String ruleId;
    private String filePath;
    private int line;
    private String codeSnippet;
    private String message;
    private String severity;
    private String remediation;
    private List<String> references;
    private List<String> tags;

    @Override
    public String toString() {
        return "Issue{" +
                "issueType='" + issueType + '\'' + "\n"+
                ", ruleId='" + ruleId + '\'' + "\n"+
                ", filePath='" + filePath + '\'' + "\n"+
                ", line=" + line + "\n"+
                ", codeSnippet='" + codeSnippet.trim() + '\'' + "\n"+
                ", message='" + message + '\'' + "\n"+
                ", severity='" + severity + '\'' + "\n"+
                ", remediation='" + remediation + '\'' + "\n"+
                ", references=" + references +"\n"+
                ", tags=" + tags +"\n"+
                '}' +"\n";
    }
}


