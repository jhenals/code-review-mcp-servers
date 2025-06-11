package dev.jhenals.mcp_semgrep_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.SemgrepSecurityCheckResult;
import dev.jhenals.mcp_semgrep_server.models.SemgrepToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dev.jhenals.mcp_semgrep_server.utils.SemgrepUtils.*;

@Slf4j
@Service
public class SecurityCheckService {

    private final ObjectMapper objectMapper= new ObjectMapper();

    public SemgrepToolResult securityCheck(Map<String, Object> input) {
        String temporaryFileAbsolutePath= null;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> codeFilesData = (List<Map<String, String>>) input.get("code_files");

            Map<String, String> codeFileMap = (Map<String, String>) input.get("code_file");
            CodeFile codeFile = new CodeFile(codeFileMap.get("filename"), codeFileMap.get("content"));

            temporaryFileAbsolutePath = createTemporaryFile(codeFile).getAbsolutePath();

            //semgrep --config "p/security-code-scan"
            ArrayList<String> commands = new ArrayList<>(Arrays.asList("semgrep",
                    "--config", "p/security-code-scan",
                    "--json",
                    "--quiet",
                    "--no-git-ignore"));
            commands.add(temporaryFileAbsolutePath);
            JsonNode output= runSemgrepService(commands, temporaryFileAbsolutePath);
            log.info(output.toPrettyString());

            SemgrepSecurityCheckResult securityCheckResult = null;

            if (!output.get("results").isEmpty()) {
                String message = output.get("results") + " security issues found in the code.\n\n" +
                        "Here are the details of the security issues found:\n\n" +
                        "<security-issues>\n" +
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString("results") +
                        "\n</security-issues>";

                securityCheckResult = new SemgrepSecurityCheckResult(message);
            } else {
                securityCheckResult = new SemgrepSecurityCheckResult("No security issues found in the code!");
            }
            return SemgrepToolResult.securityCheckSuccess(securityCheckResult);
        } catch (Exception e) {
            return SemgrepToolResult.error("INTERNAL_ERROR", e.getMessage());
        } finally {
            cleanupTempDir(temporaryFileAbsolutePath);
        }
    }


}
