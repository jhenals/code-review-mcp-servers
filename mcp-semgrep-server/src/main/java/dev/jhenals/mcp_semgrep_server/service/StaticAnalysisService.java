package dev.jhenals.mcp_semgrep_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcp_semgrep_server.models.*;
import dev.jhenals.mcp_semgrep_server.models.semgrep_parser.SemgrepResultParser;
import dev.jhenals.mcp_semgrep_server.models.SemgrepScanResult;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

import static dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils.*;

@Slf4j
@Service
public class StaticAnalysisService {

    public SemgrepToolResult semgrepScan(Map<String, Object> input){
        String temporaryFileAbsolutePath= null;

        try{
            @SuppressWarnings("unchecked")
            String config= (String) input.get("config");

            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));

            temporaryFileAbsolutePath = createTemporaryFile(codeFile).getAbsolutePath();
            ArrayList<String> commands = new ArrayList<>(Arrays.asList("semgrep",
                    "--config", config,
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            JsonNode output= runSemgrepService(commands, temporaryFileAbsolutePath);
            SemgrepScanResult results= SemgrepResultParser.parseSemgrepOutput(output);
            log.info(SemgrepResultParser.getSummary(results));

            return SemgrepToolResult.scanSuccess(results);
        } catch (Exception e){
            return SemgrepToolResult.error("INTERNAL_ERROR", e.getMessage());
        }finally {
            cleanupTempDir(temporaryFileAbsolutePath);
        }
    }

    public SemgrepToolResult semgrepScanWithCustomRule(Map<String,Object> input) {
        String temporaryFileAbsolutePath = null;
        String rulePath = null;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> codeFilesData = (List<Map<String, String>>) input.get("code_files");
            String config = (String) input.get("config");
            String rule = (String) input.get("rule");

            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));
            CodeFile ruleYaml = new CodeFile("ruleYaml.txt", (String) input.get("rule"));

            temporaryFileAbsolutePath = createTemporaryFile(codeFile).getAbsolutePath();
            rulePath = createTemporaryFile(ruleYaml).getAbsolutePath();

            ArrayList<String> commands = new ArrayList<>(Arrays.asList("semgrep",
                    "--config", rulePath,
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            commands.add(temporaryFileAbsolutePath);

            JsonNode output = runSemgrepService(commands, temporaryFileAbsolutePath);
            log.info(output.toPrettyString());

            SemgrepScanResult results = SemgrepResultParser.parseSemgrepOutput(output);
            return SemgrepToolResult.scanSuccess(results);
        } catch (Exception e) {
            return SemgrepToolResult.error("INTERNAL_ERROR", e.getMessage());
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
            cleanupTempDir(rulePath);
        }
    }

}
