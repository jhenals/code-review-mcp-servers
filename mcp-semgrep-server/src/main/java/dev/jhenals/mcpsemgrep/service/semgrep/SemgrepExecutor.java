package dev.jhenals.mcpsemgrep.service.semgrep;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.util.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class SemgrepExecutor {
    private final ProcessUtils processUtils;
    private final SemgrepConfigurationManager configurationManager;

    private static final String SEMGREP_COMMAND = "semgrep";
    private static final int SEMGREP_TIMEOUT_MINUTES = 10;

    public SemgrepExecutor(ProcessUtils processUtils, SemgrepConfigurationManager configurationManager){
        this.processUtils= processUtils;
        this.configurationManager = configurationManager;
    }

    public JsonNode executeAnalysis(String filePath, String config) throws McpAnalysisException, IOException {
        log.info("Executing Semgrep analysis on file: {} with config: {}", filePath, config);

        validateSemgrepAvailability();
        String validatedConfig = configurationManager.validateAndNormalizeConfig(config);

        // Build command
        List<String> command = buildSemgrepCommand(validatedConfig, filePath);

        // Execute using ProcessUtils
        long startTime = System.currentTimeMillis();
        try {
            JsonNode result = processUtils.builder()
                    .command(command)
                    .timeout(SEMGREP_TIMEOUT_MINUTES)
                    .expectJson(true)
                    .validateCommands(false) // We already validated above
                    .executeForJson(processUtils);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Semgrep analysis completed in {}ms", executionTime);

            return result;

        } catch (McpAnalysisException e) {
            log.error("Semgrep analysis failed for file: {}", filePath, e);
            throw new McpAnalysisException("SEMGREP_ANALYSIS_FAILED",
                    "Semgrep analysis failed: " + e.getMessage());
        }
    }

    public JsonNode executeAnalysisWithCustomRules(String filePath, String ruleFilePath) throws IOException, McpAnalysisException {
        log.info("Executing Semgrep analysis with custom rules - File: {}, Rules: {}",
                filePath, ruleFilePath);

        validateSemgrepAvailability();

        List<String> command = buildCustomRuleCommand(ruleFilePath, filePath);

        long startTime = System.currentTimeMillis();
        try {
            JsonNode result = processUtils.builder()
                    .command(command)
                    .timeout(SEMGREP_TIMEOUT_MINUTES)
                    .expectJson(true)
                    .executeForJson(processUtils);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Semgrep custom rule analysis completed in {}ms", executionTime);

            return result;

        } catch (McpAnalysisException e) {
            log.error("Semgrep custom rule analysis failed for file: {}", filePath, e);
            throw new McpAnalysisException("SEMGREP_CUSTOM_ANALYSIS_FAILED",
                    "Semgrep custom rule analysis failed: " + e.getMessage());
        }
    }

    public JsonNode executeSecurityAnalysis(String filePath, String config) throws McpAnalysisException, IOException {
        log.info("Executing Semgrep security analysis on file: {}", filePath);

        String securityConfig = (config != null && !config.trim().isEmpty()) ? config : "auto";
        return executeAnalysis(filePath, securityConfig);
    }

    public String getSemgrepVersion() throws McpAnalysisException {
        try {
            List<String> command = Arrays.asList(SEMGREP_COMMAND, "--version");

            String output = processUtils.executeCommandForString(command, 1); // 1 minute timeout

            // Extract version from output (format: "semgrep 1.x.y")
            String[] parts = output.trim().split("\\s+");
            if (parts.length >= 2) {
                return parts[1];
            } else {
                return output.trim();
            }

        } catch (Exception e) {
            throw new McpAnalysisException("VERSION_CHECK_FAILED",
                    "Failed to get Semgrep version: " + e.getMessage());
        }
    }

    public void validateSemgrepAvailability() throws McpAnalysisException {
        if (!processUtils.isCommandAvailable(SEMGREP_COMMAND)) {
            throw new McpAnalysisException("SEMGREP_NOT_AVAILABLE",
                    "Semgrep is not installed or not available in PATH. " +
                            "Please install Semgrep: https://semgrep.dev/docs/getting-started/");
        }

        log.debug("Semgrep is available");
    }

    private List<String> buildSemgrepCommand(String config, String filePath) {
        List<String> command = new ArrayList<>(Arrays.asList(
                SEMGREP_COMMAND,
                "--config", config,
                "--json",
                "--quiet",
                "--no-git-ignore"
        ));

        // Add performance optimizations
        command.add("--max-memory");
        command.add("4000"); // 4GB limit

        // Add the file path
        command.add(filePath);

        log.debug("Built Semgrep command: {}", String.join(" ", command));
        return command;
    }

    private List<String> buildCustomRuleCommand(String ruleFilePath, String filePath) {
        List<String> command = new ArrayList<>(Arrays.asList(
                SEMGREP_COMMAND,
                "--config", ruleFilePath,
                "--json",
                "--quiet",
                "--no-git-ignore"
        ));

        // Add performance optimizations
        command.add("--max-memory");
        command.add("4000");

        // Add the file path
        command.add(filePath);

        log.debug("Built Semgrep custom rule command: {}", String.join(" ", command));
        return command;
    }


    private List<String> buildRuleTestCommand(String ruleFilePath) {
        return Arrays.asList(
                SEMGREP_COMMAND,
                "--config", ruleFilePath,
                "--test"
        );
    }

    public boolean isValidRuleFile(String ruleFilePath) {
        try {
            List<String> command = buildRuleTestCommand(ruleFilePath);

            processUtils.executeCommandForString(command, 2); // 2 minute timeout

            return true;

        } catch (Exception e) {
            log.warn("Rule file validation failed for: {}", ruleFilePath, e);
            return false;
        }
    }
}
