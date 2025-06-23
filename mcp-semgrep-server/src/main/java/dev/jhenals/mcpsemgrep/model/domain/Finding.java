package dev.jhenals.mcpsemgrep.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Finding {

    @JsonProperty("check_id")
    private String checkId;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("message")
    private String message;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("start_line")
    private int startLine;

    @JsonProperty("start_column")
    private int startColumn;

    @JsonProperty("end_line")
    private int endLine;

    @JsonProperty("end_column")
    private int endColumn;

    @JsonProperty("code_snippet")
    private String codeSnippet;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public String getRuleId() {
        if (checkId == null) return null;
        String[] parts = checkId.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : checkId;
    }

    public String getCategory() {
        if (metadata != null && metadata.containsKey("category")) {
            return metadata.get("category").toString();
        }

        if (checkId != null && checkId.contains("security")) {
            return "security";
        } else if (checkId != null && checkId.contains("performance")) {
            return "performance";
        } else if (checkId != null && checkId.contains("correctness")) {
            return "correctness";
        }

        return "general";
    }

    public String getImpact() {
        if (metadata != null && metadata.containsKey("impact")) {
            return metadata.get("impact").toString();
        }
        return "UNKNOWN";
    }

    public String getLikelihood() {
        if (metadata != null && metadata.containsKey("likelihood")) {
            return metadata.get("likelihood").toString();
        }
        return "UNKNOWN";
    }

    public String getConfidence() {
        if (metadata != null && metadata.containsKey("confidence")) {
            return metadata.get("confidence").toString();
        }
        return "UNKNOWN";
    }

    public boolean isSecurityFinding() {
        return "security".equalsIgnoreCase(getCategory()) ||
                (checkId != null && checkId.toLowerCase().contains("security"));
    }

    public boolean isHighSeverity() {
        return "ERROR".equalsIgnoreCase(severity) || "HIGH".equalsIgnoreCase(severity);
    }

    /**
     * Gets the line range as a readable string.
     * Example: "42" or "42-45"
     */
    public String getLineRange() {
        if (startLine == endLine) {
            return String.valueOf(startLine);
        }
        return startLine + "-" + endLine;
    }

    public String getFileName() {
        if (filePath == null) return null;
        String[] parts = filePath.replace('\\', '/').split("/");
        return parts.length > 0 ? parts[parts.length - 1] : filePath;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getCweIds() {
        if (metadata != null && metadata.containsKey("cwe")) {
            Object cweValue = metadata.get("cwe");
            if (cweValue instanceof java.util.List) {
                return (java.util.List<String>) cweValue;
            }
        }
        return java.util.Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getOwaspCategories() {
        if (metadata != null && metadata.containsKey("owasp")) {
            Object owaspValue = metadata.get("owasp");
            if (owaspValue instanceof java.util.List) {
                return (java.util.List<String>) owaspValue;
            }
        }
        return java.util.Collections.emptyList();
    }

    public String getSummary() {
        return String.format("[%s] %s at %s:%s",
                severity,
                getRuleId(),
                getFileName(),
                getLineRange());
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
