package dev.jhenals.mcpsemgrep.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.parser.SemgrepResultParser;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.service.semgrep.SemgrepExecutor;
import dev.jhenals.mcpsemgrep.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class CodeAnalysisService {
    private final SemgrepExecutor semgrepExecutor;
    private final SemgrepResultParser resultParser;
    private final FileUtils fileUtils;

    public CodeAnalysisService(SemgrepExecutor semgrepExecutor,
                               SemgrepResultParser resultParser,
                               FileUtils fileUtils) {
        this.semgrepExecutor = semgrepExecutor;
        this.resultParser = resultParser;
        this.fileUtils = fileUtils;
    }


    public AnalysisResult analyzeCode(CodeAnalysisRequest request)
            throws McpAnalysisException, IOException {
        log.info("Starting code analysis for: {}", request.getCodeFile().getFileName());

        File tempFile = null;
        try {
            // Step 1: Create temporary file
            tempFile = fileUtils.createTemporaryFile(request.getCodeFile());
            log.debug("Created temporary file: {}", tempFile.getAbsolutePath());

            // Step 2: Execute Semgrep analysis
            JsonNode rawResult = semgrepExecutor.executeAnalysis(
                    tempFile.getAbsolutePath(),
                    request.getConfig() != null ? request.getConfig() : "auto"
            );

            // Step 3: Parse results
            AnalysisResult result = resultParser.parseAnalysisResult(rawResult);

            log.info("Code analysis completed - Found {} findings", result.getFindingCount());
            return result;

        } catch (Exception e) {
            log.error("Error during code analysis for: {}", request.getCodeFile().getFileName(), e);
            throw new McpAnalysisException("ANALYSIS_FAILED",
                    "Failed to analyze code: " + e.getMessage());
        } finally {
            fileUtils.cleanupTempFile(tempFile);
        }
    }

    public AnalysisResult analyzeCodeWithCustomRules(CodeAnalysisRequest request)
            throws McpAnalysisException, IOException {
        log.info("Starting custom rule analysis for: {}", request.getCodeFile().getFileName());

        if (request.getCustomRule() == null || request.getCustomRule().trim().isEmpty()) {
            throw new McpAnalysisException("MISSING_CUSTOM_RULE",
                    "Custom rule is required for custom rule analysis");
        }

        File tempCodeFile = null;
        File tempRuleFile = null;
        try {
            // Step 1: Create temporary files
            tempCodeFile = fileUtils.createTemporaryFile(request.getCodeFile());
            tempRuleFile = fileUtils.createTemporaryRuleFile(request.getCustomRule());

            log.debug("Created temporary files - Code: {}, Rule: {}",
                    tempCodeFile.getAbsolutePath(), tempRuleFile.getAbsolutePath());

            // Step 2: Execute Semgrep with custom rules
            JsonNode rawResult = semgrepExecutor.executeAnalysisWithCustomRules(
                    tempCodeFile.getAbsolutePath(),
                    tempRuleFile.getAbsolutePath()
            );

            // Step 3: Parse results
            AnalysisResult result = resultParser.parseAnalysisResult(rawResult);

            log.info("Custom rule analysis completed - Found {} findings", result.getFindingCount());
            return result;

        } catch (Exception e) {
            log.error("Error during custom rule analysis for: {}",
                    request.getCodeFile().getFileName(), e);
            throw new McpAnalysisException("CUSTOM_ANALYSIS_FAILED",
                    "Failed to analyze code with custom rules: " + e.getMessage());
        } finally {
            fileUtils.cleanupTempFile(tempCodeFile);
            fileUtils.cleanupTempFile(tempRuleFile);
        }
    }
    /*
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SemgrepExecutor semgrepExecutor;
    private final SemgrepResultParser resultParser;
    private final FileUtils fileUtils;

    public CodeAnalysisService(SemgrepExecutor semgrepExecutor, SemgrepResultParser resultParser, FileUtils fileUtils) {
        this.semgrepExecutor = semgrepExecutor;
        this.resultParser = resultParser;
        this.fileUtils = fileUtils;
    }


    public AnalysisResult analyzeCode(CodeAnalysisRequest request) throws McpAnalysisException, IOException {
        log.info("Starting code analysis for: {}", request.getCodeFile().getFileName());
        File tempFile= null;
        String temporaryFileAbsolutePath= null;

        try{
            tempFile= fileUtils.createTemporaryFile(request.getCodeFile());

            JsonNode rawResult = semgrepExecutor.executeAn6 alysis(
                    tempFile.getAbsolutePath(),
                    request.getConfig()
            );

            String config= validateConfig((String) input.get("McpConfiguration"));

            @SuppressWarnings("unchecked")
            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));

            temporaryFileAbsolutePath = createTemporaryFile(codeFile).getAbsolutePath();
            ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                    "mcpsemgrep",
                    "--McpConfiguration", config,
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            JsonNode output= runSemgrepService(commands, temporaryFileAbsolutePath);
            AnalysisResult analysisResult = SemgrepResultParser.parseSemgrepOutput(output);
            log.info(SemgrepResultParser.getSummary(analysisResult));

            return analysisResult;
        } catch (McpAnalysisException e){
            throw new McpAnalysisException("INTERNAL_ERROR", e.getMessage());
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
        }
    }

    public AnalysisResult analyzeCodeWithCustomRules(Map<String,Object> input) throws McpAnalysisException, IOException {
        String temporaryFileAbsolutePath = null;
        String rulePath = null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));
            CodeFile ruleYaml = new CodeFile("ruleYaml.txt", (String) input.get("rule"));

            temporaryFileAbsolutePath = createTemporaryFile(codeFile).getAbsolutePath();
            rulePath = createTemporaryFile(ruleYaml).getAbsolutePath();

            ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                    "mcpsemgrep",
                    "--McpConfiguration", rulePath,
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            commands.add(temporaryFileAbsolutePath);


            JsonNode output = runSemgrepService(commands, temporaryFileAbsolutePath);
            log.info("Json output: {}", output.toPrettyString());

            return SemgrepResultParser.parseSemgrepOutput(output);

        } catch (McpAnalysisException e) {
            throw new McpAnalysisException("INTERNAL_ERROR", e.getMessage());
        } catch (IOException e){
            throw new IOException(e.getMessage());
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
            cleanupTempDir(rulePath);
        }
    }

     */

}
