package dev.jhenals.mcp_semgrep_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.SemgrepSecurityCheckResult;
import dev.jhenals.mcp_semgrep_server.models.*;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import dev.jhenals.mcp_semgrep_server.utils.Utils;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static dev.jhenals.mcp_semgrep_server.utils.Utils.*;

@Service
public class SemgrepService {

    // Global state
    private static volatile String semgrepExecutable = null;
    private static final ReentrantLock semgrepLock = new ReentrantLock();

    private final ObjectMapper objectMapper = new ObjectMapper();


    public String getSemgrepRuleSchema(){
        return null;
        //TODO
    }

    private String runSemgrep(List<String> args) throws McpError {
        return Utils.runSemgrep(args, semgrepExecutable, semgrepLock);
    }

    //TODO: use generics (<T>) instead of Object (root of hierarchy)
    public SemgrepResult semgrepScan(Map<String, Object> input){
        String tempDir= null;
        try{
            @SuppressWarnings("unchecked")
            List<Map<String, String>> codeFilesData = (List<Map<String, String>>) input.get("code_files");
            String config= (String) input.get("config");

            List<CodeFile> codeFiles= new ArrayList<>();
            for(Map<String, String> codeFile: codeFilesData){
                codeFiles.add(new CodeFile(codeFile.get("filename"), codeFile.get("content")));
            }

            config = validateConfig(config);
            validateCodeFiles(codeFiles);

            tempDir= createTempFilesFromCodeContent(codeFiles);
            List<String> args= getSemgrepScanArgs(tempDir, config);
            String output= runSemgrep(args);

            SemgrepScanResult results= objectMapper.readValue(output, SemgrepScanResult.class);
            removeTempDirFromResults(results, tempDir);

            return SemgrepResult.scanSuccess(results);
        } catch (McpError e) {
            return SemgrepResult.error(e.getCode(), e.getMessage());
        }catch (Exception e){
            return SemgrepResult.error("INERNAL_ERROR", e.getMessage());
        }finally {
            cleanupTempDir(tempDir);
        }
    }

    public SemgrepResult semgrepScanWithCustomeRule(Map<String, Object> input){
        String tempDir =  null;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> codeFilesData = (List<Map<String, String>>) input.get("code_files");
            String rule = (String) input.get("rule");

            List<CodeFile> codeFiles = new ArrayList<>();
            for (Map<String, String> fileData : codeFilesData) {
                codeFiles.add(new CodeFile(fileData.get("filename"), fileData.get("content")));
            }

            validateCodeFiles(codeFiles);

            tempDir = createTempFilesFromCodeContent(codeFiles);

            // Write rule to file
            String ruleFilePath = Paths.get(tempDir, "rule.yaml").toString();
            Files.write(Paths.get(ruleFilePath), rule.getBytes());

            List<String> args = getSemgrepScanArgs(tempDir, ruleFilePath);
            String output = runSemgrep(args);

            SemgrepScanResult results = objectMapper.readValue(output, SemgrepScanResult.class);
            removeTempDirFromResults(results, tempDir);

            return SemgrepResult.scanSuccess(results);
        } catch (McpError e) {
            return SemgrepResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return SemgrepResult.error("INTERNAL_ERROR", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    public SemgrepResult securityCheck(Map<String, Object> input){
        String tempDir= null;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> codeFilesData = (List<Map<String, String>>) input.get("code_files");

            List<CodeFile> codeFiles = new ArrayList<>();
            for (Map<String, String> fileData : codeFilesData) {
                codeFiles.add(new CodeFile(fileData.get("filename"), fileData.get("content")));
            }

            validateCodeFiles(codeFiles);

            tempDir = createTempFilesFromCodeContent(codeFiles);
            List<String> args = getSemgrepScanArgs(tempDir, null);
            String output = runSemgrep(args);

            SemgrepScanResult results = objectMapper.readValue(output, SemgrepScanResult.class);
            removeTempDirFromResults(results, tempDir);

            SemgrepSecurityCheckResult securityCheckResult = null;

            if (!results.getResults().isEmpty()) {
                String message = results.getResults().size() + " security issues found in the code.\n\n" +
                        "Here are the details of the security issues found:\n\n" +
                        "<security-issues>\n" +
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results) +
                        "\n</security-issues>";

                securityCheckResult = new SemgrepSecurityCheckResult(message);
            } else {
                securityCheckResult = new SemgrepSecurityCheckResult("No security issues found in the code!");
            }
            return SemgrepResult.securityCheckSuccess(securityCheckResult);
        } catch (McpError e) {
            return SemgrepResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return SemgrepResult.error("INTERNAL_ERROR", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    public List<String> getSupportLanguages() throws McpError {
        List<String> args = Arrays.asList("show", "supported-languages", "--experimental");
        String output = runSemgrep(args);

        return Arrays.stream(output.split("\n"))
                .map(String::trim)
                .filter(lang -> !lang.isEmpty())
                .collect(Collectors.toList());
    }

    public Map<String,String> getAbstractSyntaxTree(CodeWithLanguage input) {
        String tempDir = null;
        try {
            tempDir = Files.createTempDirectory("semgrep_ast_").toString();
            String tempFilePath = Paths.get(tempDir, "code.txt").toString();

            Files.write(Paths.get(tempFilePath), input.getContent().getBytes());

            List<String> args = Arrays.asList(
                    "--experimental",
                    "--dump-ast",
                    "-l", input.getLanguage(),
                    "--json",
                    tempFilePath
            );
            return Map.of("Abstract Syntax Tree",runSemgrep(args));
        } catch (McpError e) {
            return Map.of("error", e.getCode(), "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "INTERNAL_ERROR", "message", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

}
