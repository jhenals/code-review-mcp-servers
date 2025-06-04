package dev.jhenals.analyzer_server.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class Issue {
    public String issueType;
    public String ruleId;
    public String filePath;
    public int line;
    public String codeSnippet;
    public String message;
    public String severity;
    public String remediation;
    public List<String> references;
    public List<String> tags;

    @Override
    public String toString() {
        return "***Issue{" +
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


