package dev.jhenals.mcp_semgrep_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.models.*;
import dev.jhenals.mcp_semgrep_server.models.semgrep_parser.SemgrepResultParser;
import dev.jhenals.mcp_semgrep_server.models.StaticAnalysisResult;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils.*;

@Slf4j
@Service
public class StaticAnalysisService {
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public StaticAnalysisResult semgrepScan(Map<String, Object> input) throws McpError, IOException {
        String temporaryFileAbsolutePath= null;

        try{
            String config= validateConfig((String) input.get("config"));

            @SuppressWarnings("unchecked")
            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));

            temporaryFileAbsolutePath = createTemporaryFile(codeFile).getAbsolutePath();
            ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                    "semgrep",
                    "--config", config,
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            JsonNode output= runSemgrepService(commands, temporaryFileAbsolutePath);
            StaticAnalysisResult staticAnalysisResult= SemgrepResultParser.parseSemgrepOutput(output);
            log.info(SemgrepResultParser.getSummary(staticAnalysisResult));

            return staticAnalysisResult;
        } catch (McpError e){
            throw new McpError("INTERNAL_ERROR", e.getMessage());
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
        }
    }

    public StaticAnalysisResult semgrepScanWithCustomRule(Map<String,Object> input) throws McpError, IOException {
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
                    "semgrep",
                    "--config", rulePath,
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            commands.add(temporaryFileAbsolutePath);


            JsonNode output = runSemgrepService(commands, temporaryFileAbsolutePath);
            log.info("Json output: {}", output.toPrettyString());

            return SemgrepResultParser.parseSemgrepOutput(output);

        } catch (McpError e) {
            throw new McpError("INTERNAL_ERROR", e.getMessage());
        } catch (IOException e){
            throw new IOException(e.getMessage());
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
            cleanupTempDir(rulePath);
        }
    }

}
