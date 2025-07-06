package dev.jhenals.mcpsemgrep.service.semgrep;

import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigurationResolverTest {

    @Mock
    private SemgrepConfigurationManager configManager;

    @InjectMocks
    private ConfigurationResolver resolver;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper to create CodeFile with filename
    private CodeFile codeFile(String filename) {
        CodeFile cf = new CodeFile();
        cf.setFileName(filename);
        return cf;
    }

    // --- Tests for resolveGeneralConfig ---

    @Test
    void resolveGeneralConfig_returnsValidatedUserConfig_whenUserConfigValid() throws McpAnalysisException {
        String userConfig = "p/custom-config";
        CodeFile file = codeFile("test.py");

        when(configManager.validateAndNormalizeConfig(userConfig)).thenReturn("p/custom-config-normalized");

        String result = resolver.resolveGeneralConfig(userConfig, file);

        assertEquals("p/custom-config-normalized", result);
        verify(configManager).validateAndNormalizeConfig(userConfig);
    }

    @Test
    void resolveGeneralConfig_fallsBackToSmartDetection_whenUserConfigInvalid() throws McpAnalysisException {
        String userConfig = "invalid-config";
        CodeFile file = codeFile("test.py");

        when(configManager.validateAndNormalizeConfig(userConfig)).thenThrow(new McpAnalysisException("Invalid", "Invalid configuration"));

        String result = resolver.resolveGeneralConfig(userConfig, file);

        // For python general config, expect "p/python"
        assertEquals("p/python", result);
        verify(configManager).validateAndNormalizeConfig(userConfig);
    }

    @Test
    void resolveGeneralConfig_fallsBackToDefault_whenNoUserConfigAndUnknownFile() {
        String userConfig = null;
        CodeFile file = codeFile("file.unknownext");

        String result = resolver.resolveGeneralConfig(userConfig, file);

        assertEquals("auto", result);
    }

    @Test
    void resolveGeneralConfig_fallsBackToDefault_whenNoUserConfigAndNullFile() {
        String userConfig = null;

        String result = resolver.resolveGeneralConfig(userConfig, null);

        assertEquals("auto", result);
    }

    // --- Tests for resolveSecurityConfig ---

    @Test
    void resolveSecurityConfig_returnsValidatedUserConfig_whenUserConfigValid() throws McpAnalysisException {
        String userConfig = "r/custom-security";
        CodeFile file = codeFile("test.js");

        when(configManager.validateAndNormalizeConfig(userConfig)).thenReturn("r/custom-security-normalized");

        String result = resolver.resolveSecurityConfig(userConfig, file);

        assertEquals("r/custom-security-normalized", result);
        verify(configManager).validateAndNormalizeConfig(userConfig);
    }

    @Test
    void resolveSecurityConfig_fallsBackToSmartDetection_whenUserConfigInvalid() throws McpAnalysisException {
        String userConfig = "invalid-security-config";
        CodeFile file = codeFile("test.js");

        when(configManager.validateAndNormalizeConfig(userConfig)).thenThrow(new McpAnalysisException("Invalid", "Invalid configuration"));

        String result = resolver.resolveSecurityConfig(userConfig, file);

        // For javascript security config, expect "r/javascript.lang.security"
        assertEquals("r/javascript.lang.security", result);
        verify(configManager).validateAndNormalizeConfig(userConfig);
    }

    @Test
    void resolveSecurityConfig_fallsBackToDefault_whenNoUserConfigAndUnknownFile() {
        String userConfig = null;
        CodeFile file = codeFile("file.unknownext");

        String result = resolver.resolveSecurityConfig(userConfig, file);

        assertEquals("p/security-audit", result);
    }

    @Test
    void resolveSecurityConfig_fallsBackToDefault_whenNoUserConfigAndNullFile() {
        String userConfig = null;

        String result = resolver.resolveSecurityConfig(userConfig, null);

        assertEquals("p/security-audit", result);
    }

    // --- Tests for getRecommendations ---

    @Test
    void getRecommendations_returnsDefaults_whenCodeFileNull() {
        ConfigurationResolver.ConfigurationRecommendation rec = resolver.getRecommendations(null);

        assertEquals("auto", rec.getPrimaryRecommendation());
        assertEquals("p/security-audit", rec.getSecurityRecommendation());
        assertEquals("unknown", rec.getDetectedLanguage());
        assertNull(rec.getFilename());
    }

    @Test
    void getRecommendations_returnsDefaults_whenFileNameNull() {
        CodeFile file = new CodeFile(); // filename null
        ConfigurationResolver.ConfigurationRecommendation rec = resolver.getRecommendations(file);

        assertEquals("auto", rec.getPrimaryRecommendation());
        assertEquals("p/security-audit", rec.getSecurityRecommendation());
        assertEquals("unknown", rec.getDetectedLanguage());
        assertNull(rec.getFilename());
        System.out.println( "[DEBUG]"+rec.getAlternativeConfigs());
    }

    @Test
    void getRecommendations_returnsCorrectRecommendations_forPythonFile() {
        CodeFile file = codeFile("script.py");

        ConfigurationResolver.ConfigurationRecommendation rec = resolver.getRecommendations(file);

        assertEquals("p/python", rec.getPrimaryRecommendation());
        assertEquals("r/python.lang.security", rec.getSecurityRecommendation());
        assertEquals("python", rec.getDetectedLanguage());
        assertEquals("script.py", rec.getFilename());
        List<String> alternatives = rec.getAlternativeConfigs();
        assertTrue(alternatives.contains("p/python"));
        assertTrue(alternatives.contains("r/python.lang.security"));
        assertTrue(alternatives.contains("p/owasp-top-10"));
    }

    @Test
    void getRecommendations_returnsCorrectRecommendations_forUnknownFile() {
        CodeFile file = codeFile("file.unknownext");

        ConfigurationResolver.ConfigurationRecommendation rec = resolver.getRecommendations(file);

        assertEquals("auto", rec.getPrimaryRecommendation());
        assertEquals("p/security-audit", rec.getSecurityRecommendation());
        assertEquals("unknown", rec.getDetectedLanguage());
        assertEquals("file.unknownext", rec.getFilename());
        List<String> alternatives = rec.getAlternativeConfigs();
        assertTrue(alternatives.contains("auto"));
        assertTrue(alternatives.contains("p/security-audit"));
        assertTrue(alternatives.contains("p/owasp-top-10"));
    }

    // --- Tests for isConfigurationValid ---

    @Test
    void isConfigurationValid_returnsTrue_whenConfigValid() throws McpAnalysisException {
        String config = "p/python";

        when(configManager.validateAndNormalizeConfig(config)).thenReturn(config);

        assertTrue(resolver.isConfigurationValid(config));
        verify(configManager).validateAndNormalizeConfig(config);
    }

    @Test
    void isConfigurationValid_returnsFalse_whenConfigInvalid() throws McpAnalysisException {
        String config = "invalid-config";

        when(configManager.validateAndNormalizeConfig(config)).thenThrow(new McpAnalysisException("Invalid", "Invalid configuration"));

        assertFalse(resolver.isConfigurationValid(config));
        verify(configManager).validateAndNormalizeConfig(config);
    }

    // --- Tests for private helper getFileExtension (indirectly tested) ---

    @Test
    void detectLanguage_returnsCorrectLanguage_forKnownExtensions() {
        assertEquals("python", resolver.detectLanguage("file.py"));
        assertEquals("javascript", resolver.detectLanguage("file.js"));
        assertEquals("javascript", resolver.detectLanguage("file.jsx"));
        assertEquals("typescript", resolver.detectLanguage("file.ts"));
        assertEquals("typescript", resolver.detectLanguage("file.tsx"));
        assertEquals("java", resolver.detectLanguage("file.java"));
        assertEquals("go", resolver.detectLanguage("file.go"));
        assertEquals("yaml", resolver.detectLanguage("config.yaml"));
        assertEquals("yaml", resolver.detectLanguage("config.yml"));
    }

    @Test
    void detectLanguage_returnsUnknown_forUnknownExtension() {
        assertEquals("unknown", resolver.detectLanguage("file.unknownext"));
        assertEquals("unknown", resolver.detectLanguage("file"));
        assertEquals("unknown", resolver.detectLanguage(null));
    }
}
