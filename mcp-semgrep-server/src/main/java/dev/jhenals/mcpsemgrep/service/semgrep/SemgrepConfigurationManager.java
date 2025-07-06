package dev.jhenals.mcpsemgrep.service.semgrep;

import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class SemgrepConfigurationManager {
    private static final List<String> VALID_PRESET_CONFIGS = Arrays.asList(
            "auto",
            "p/security",
            "p/owasp-top-10",
            "p/cwe-top-25",
            "r/java",
            "r/javascript",
            "r/python",
            "r/go",
            "r/typescript"
    );

    public String validateAndNormalizeConfig(String config) throws McpAnalysisException {
        if (config == null || config.trim().isEmpty()) {
            log.debug("No config provided, defaulting to 'auto'");
            return "auto";
        }

        String normalizedConfig = config.trim().toLowerCase();

        // Check if it's a preset configuration
        if (VALID_PRESET_CONFIGS.contains(normalizedConfig)) {
            return normalizedConfig;
        }

        // Check if it's a custom path or URL
        if (normalizedConfig.startsWith("http") || normalizedConfig.contains("/") || normalizedConfig.contains("\\")) {
            log.debug("Using custom config path: {}", normalizedConfig);
            return config.trim(); // Preserve original case for paths
        }

        // If none of the above, try to suggest a valid alternative
        String suggestion = findSimilarConfig(normalizedConfig);
        if (suggestion != null) {
            throw new McpAnalysisException("INVALID_CONFIG",
                    String.format("Invalid configuration '%s'. Did you mean '%s'? Valid preset configs: %s",
                            config, suggestion, String.join(", ", VALID_PRESET_CONFIGS)));
        } else {
            throw new McpAnalysisException("INVALID_CONFIG",
                    String.format("Invalid configuration '%s'. Valid preset configs: %s",
                            config, String.join(", ", VALID_PRESET_CONFIGS)));
        }
    }

    private String findSimilarConfig(String config) {
        // Simple similarity check
        for (String validConfig : VALID_PRESET_CONFIGS) {
            if (validConfig.contains(config) || config.contains(validConfig)) {
                return validConfig;
            }
        }

        // Check for common mistakes
        return switch (config) {
            case "security" -> "p/security";
            case "owasp" -> "p/owasp-top-10";
            case "cwe" -> "p/cwe-top-25";
            default -> null;
        };

    }

    public List<String> getAvailableConfigs() {
        return new ArrayList<>(VALID_PRESET_CONFIGS);
    }
}
