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

    // ========================================
    // Computed Properties
    // ========================================

    /**
     * Gets the rule ID without the full path prefix.
     * Example: "java.lang.security.audit.crypto.ssl.disabling-verification" → "disabling-verification"
     */
    public String getRuleId() {
        if (checkId == null) return null;
        String[] parts = checkId.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : checkId;
    }

    /**
     * Gets the technology/language from the check ID.
     * Example: "java.lang.security.audit.crypto" → "java"
     */
    public String getTechnology() {
        if (checkId == null) return "unknown";
        String[] parts = checkId.split("\\.");
        return parts.length > 0 ? parts[0] : "unknown";
    }

    /**
     * Gets the category from metadata or derives from check ID.
     */
    public String getCategory() {
        if (metadata != null && metadata.containsKey("category")) {
            return metadata.get("category").toString();
        }

        // Try to derive from check ID
        if (checkId != null && checkId.contains("security")) {
            return "security";
        } else if (checkId != null && checkId.contains("performance")) {
            return "performance";
        } else if (checkId != null && checkId.contains("correctness")) {
            return "correctness";
        }

        return "general";
    }

    /**
     * Gets the impact level from metadata.
     */
    public String getImpact() {
        if (metadata != null && metadata.containsKey("impact")) {
            return metadata.get("impact").toString();
        }
        return "UNKNOWN";
    }

    /**
     * Gets the likelihood from metadata.
     */
    public String getLikelihood() {
        if (metadata != null && metadata.containsKey("likelihood")) {
            return metadata.get("likelihood").toString();
        }
        return "UNKNOWN";
    }

    /**
     * Gets the confidence level from metadata.
     */
    public String getConfidence() {
        if (metadata != null && metadata.containsKey("confidence")) {
            return metadata.get("confidence").toString();
        }
        return "UNKNOWN";
    }

    /**
     * Checks if this finding is security-related.
     */
    public boolean isSecurityFinding() {
        return "security".equalsIgnoreCase(getCategory()) ||
                (checkId != null && checkId.toLowerCase().contains("security"));
    }

    /**
     * Checks if this finding is high severity (ERROR level).
     */
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

    /**
     * Gets the file name from the full file path.
     */
    public String getFileName() {
        if (filePath == null) return null;
        String[] parts = filePath.replace('\\', '/').split("/");
        return parts.length > 0 ? parts[parts.length - 1] : filePath;
    }

    /**
     * Gets CWE (Common Weakness Enumeration) identifiers from metadata.
     */
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

    /**
     * Gets OWASP category from metadata.
     */
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

    /**
     * Creates a readable summary of the finding.
     */
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
