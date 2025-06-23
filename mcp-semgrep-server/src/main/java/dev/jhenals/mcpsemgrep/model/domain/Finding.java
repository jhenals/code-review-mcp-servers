package dev.jhenals.mcpsemgrep.model.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Finding {

    @JsonProperty("rule_id")
    private String ruleId;

    private String message;

    private String severity;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("line_number")
    private Integer lineNumber;

    @JsonProperty("column_number")
    private Integer columnNumber;

    @JsonProperty("code_snippet")
    private String codeSnippet;

    @JsonProperty("rule_name")
    private String ruleName;

    public boolean isHighSeverity() {
        return "ERROR".equalsIgnoreCase(severity);
    }

    public boolean isSecurityRelated() {
        return ruleId != null &&
                (ruleId.contains("security") ||
                        ruleId.contains("sqli") ||
                        ruleId.contains("xss") ||
                        ruleId.contains("injection") ||
                        ruleId.contains("crypto"));
    }
}
