package dev.jhenals.mcpsemgrep.service.semgrep;

import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConfigurationResolver {

    @Autowired
    private SemgrepConfigurationManager configManager;

    // Language detection mappings
    private static final Map<String, String> EXTENSION_TO_LANGUAGE = new HashMap<>();
    static {
        EXTENSION_TO_LANGUAGE.put("py", "python");
        EXTENSION_TO_LANGUAGE.put("js", "javascript");
        EXTENSION_TO_LANGUAGE.put("jsx", "javascript");
        EXTENSION_TO_LANGUAGE.put("ts", "typescript");
        EXTENSION_TO_LANGUAGE.put("tsx", "typescript");
        EXTENSION_TO_LANGUAGE.put("java", "java");
        EXTENSION_TO_LANGUAGE.put("go", "go");
        //EXTENSION_TO_LANGUAGE.put("dockerfile", "docker");
        EXTENSION_TO_LANGUAGE.put("yaml", "yaml");
        EXTENSION_TO_LANGUAGE.put("yml", "yaml");
    }

    // Default configurations for different analysis types
    private static final String DEFAULT_GENERAL_CONFIG = "auto";
    private static final String DEFAULT_SECURITY_CONFIG = "p/security-audit";

    /**
     * Resolve configuration for general code analysis
     */
    public String resolveGeneralConfig(String userConfig, CodeFile codeFile) {
        log.debug("Resolving general config - user: '{}', file: '{}'",
                userConfig, codeFile != null ? codeFile.getFileName() : "null");

        // 1. Try user-provided config first
        String resolvedConfig = tryUserConfig(userConfig);
        if (resolvedConfig != null) {
            return resolvedConfig;
        }

        // 2. Try smart detection from file
        resolvedConfig = trySmartDetection(codeFile, false);
        if (resolvedConfig != null) {
            return resolvedConfig;
        }

        // 3. Fallback to default
        log.debug("Using default general config: {}", DEFAULT_GENERAL_CONFIG);
        return DEFAULT_GENERAL_CONFIG;
    }

    /**
     * Resolve configuration for security analysis (prioritizes security rulesets)
     */
    public String resolveSecurityConfig(String userConfig, CodeFile codeFile) {
        log.debug("Resolving security config - user: '{}', file: '{}'",
                userConfig, codeFile != null ? codeFile.getFileName() : "null");

        // 1. Try user-provided config first
        String resolvedConfig = tryUserConfig(userConfig);
        if (resolvedConfig != null) {
            return resolvedConfig;
        }

        // 2. Try security-focused smart detection
        resolvedConfig = trySmartDetection(codeFile, true);
        if (resolvedConfig != null) {
            return resolvedConfig;
        }

        // 3. Fallback to default security config
        log.debug("Using default security config: {}", DEFAULT_SECURITY_CONFIG);
        return DEFAULT_SECURITY_CONFIG;
    }

    /**
     * Get configuration recommendations for a file
     */
    public ConfigurationRecommendation getRecommendations(CodeFile codeFile) {
        if (codeFile == null || codeFile.getFileName() == null) {
            return ConfigurationRecommendation.builder()
                    .primaryRecommendation(DEFAULT_GENERAL_CONFIG)
                    .securityRecommendation(DEFAULT_SECURITY_CONFIG)
                    .detectedLanguage("unknown")
                    .build();
        }

        String language = detectLanguage(codeFile.getFileName());
        String generalConfig = getLanguageSpecificConfig(language, false);
        String securityConfig = getLanguageSpecificConfig(language, true);

        return ConfigurationRecommendation.builder()
                .primaryRecommendation(generalConfig)
                .securityRecommendation(securityConfig)
                .detectedLanguage(language)
                .filename(codeFile.getFileName())
                .alternativeConfigs(getAlternativeConfigs(language))
                .build();
    }

    /**
     * Validate if a configuration is available and working
     */
    public boolean isConfigurationValid(String config) {
        try {
            configManager.validateAndNormalizeConfig(config);
            return true;
        } catch (McpAnalysisException e) {
            log.debug("Configuration '{}' is invalid: {}", config, e.getMessage());
            return false;
        }
    }

    // Private helper methods

    private String tryUserConfig(String userConfig) {
        if (userConfig == null || userConfig.trim().isEmpty()) {
            return null;
        }

        try {
            String validated = configManager.validateAndNormalizeConfig(userConfig);
            log.debug("User config '{}' validated as '{}'", userConfig, validated);
            return validated;
        } catch (McpAnalysisException e) {
            log.warn("Invalid user config '{}': {}", userConfig, e.getMessage());
            return null;
        }
    }

    private String trySmartDetection(CodeFile codeFile, boolean securityFocused) {
        if (codeFile == null || codeFile.getFileName() == null) {
            return null;
        }

        String language = detectLanguage(codeFile.getFileName());
        String config = getLanguageSpecificConfig(language, securityFocused);

        log.debug("Smart detection for '{}': language='{}', config='{}', security='{}'",
                codeFile.getFileName(), language, config, securityFocused);

        return config;
    }

    String detectLanguage(String filename) {
        if (filename == null) {
            return "unknown";
        }

        String extension = getFileExtension(filename.toLowerCase());
        return EXTENSION_TO_LANGUAGE.getOrDefault(extension, "unknown");
    }

    private String getLanguageSpecificConfig(String language, boolean securityFocused) {
        if ("unknown".equals(language)) {
            return securityFocused ? DEFAULT_SECURITY_CONFIG : DEFAULT_GENERAL_CONFIG;
        }

        if (securityFocused) {
            return switch (language) {
                case "python" -> "r/python.lang.security";
                case "javascript" -> "r/javascript.lang.security";
                case "java" -> "r/java.lang.security";
                case "typescript" -> "p/typescript";
                default -> DEFAULT_SECURITY_CONFIG;
            };
        } else {
            return switch (language) {
                case "python" -> "p/python";
                case "javascript" -> "p/javascript";
                case "java" -> "p/java";
                case "typescript" -> "p/typescript";
                case "go" -> "p/go";
                case "docker" -> "p/dockerfile";
                default -> DEFAULT_GENERAL_CONFIG;
            };
        }
    }

    private List<String> getAlternativeConfigs(String language) {
        return switch (language) {
            case "python" -> Arrays.asList("p/python", "r/python.lang.security", "p/owasp-top-10");
            case "javascript" -> Arrays.asList("p/javascript", "r/javascript.lang.security", "p/owasp-top-10");
            case "java" -> Arrays.asList("p/java", "r/java.lang.security", "p/owasp-top-10");
            default -> Arrays.asList("auto", "p/security-audit", "p/owasp-top-10");
        };
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // Inner class for configuration recommendations
    @Getter
    public static class ConfigurationRecommendation {
        // Getters
        private String primaryRecommendation;
        private String securityRecommendation;
        private String detectedLanguage;
        private String filename;
        private List<String> alternativeConfigs;

        public static ConfigurationRecommendationBuilder builder() {
            return new ConfigurationRecommendationBuilder();
        }

        // Builder pattern implementation
        public static class ConfigurationRecommendationBuilder {
            private String primaryRecommendation;
            private String securityRecommendation;
            private String detectedLanguage;
            private String filename;
            private List<String> alternativeConfigs;

            public ConfigurationRecommendationBuilder primaryRecommendation(String primaryRecommendation) {
                this.primaryRecommendation = primaryRecommendation;
                return this;
            }

            public ConfigurationRecommendationBuilder securityRecommendation(String securityRecommendation) {
                this.securityRecommendation = securityRecommendation;
                return this;
            }

            public ConfigurationRecommendationBuilder detectedLanguage(String detectedLanguage) {
                this.detectedLanguage = detectedLanguage;
                return this;
            }

            public ConfigurationRecommendationBuilder filename(String filename) {
                this.filename = filename;
                return this;
            }

            public ConfigurationRecommendationBuilder alternativeConfigs(List<String> alternativeConfigs) {
                this.alternativeConfigs = alternativeConfigs;
                return this;
            }

            public ConfigurationRecommendation build() {
                ConfigurationRecommendation recommendation = new ConfigurationRecommendation();
                recommendation.primaryRecommendation = this.primaryRecommendation;
                recommendation.securityRecommendation = this.securityRecommendation;
                recommendation.detectedLanguage = this.detectedLanguage;
                recommendation.filename = this.filename;
                recommendation.alternativeConfigs = this.alternativeConfigs;
                return recommendation;
            }
        }

    }
}