package dev.jhenals.mcp_semgrep_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.StaticAnalysisResult;
import dev.jhenals.mcp_semgrep_server.models.semgrep_parser.SemgrepResultParser;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils.*;

@Slf4j
@Service
public class SecurityCheckService {

    private final ObjectMapper objectMapper= new ObjectMapper();

    public StaticAnalysisResult securityCheck(Map<String, Object> input) throws McpError {
        String temporaryFileAbsolutePath= null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));

            temporaryFileAbsolutePath = createTemporaryFile(codeFile).getAbsolutePath();

            //semgrep --config p/security-code-scan
            ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                    "semgrep",
                    "--config", "p/security-code-scan",
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            commands.add(temporaryFileAbsolutePath);
            JsonNode output= runSemgrepService(commands, temporaryFileAbsolutePath);
            log.info("Json output: {}", output.toPrettyString());
            return SemgrepResultParser.parseSemgrepOutput(output);
        } catch (IOException e) {
            throw new McpError("INTERNAL_ERROR", e.getMessage());
        } catch (McpError e) {
            throw new RuntimeException(e);
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
        }
    }


}
