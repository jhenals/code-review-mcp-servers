package dev.jhenals.mcp_semgrep_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.models.*;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;

import java.io.*;
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

    /*

    public String analyzeCode(String javaSourceCode) throws IOException {
        StaticAnalysisResult result= new StaticAnalysisResult();
        // 1. Create temporary directory
        File tempDir = Files.createTempDirectory("semgrep-src").toFile();

        // 2. Create file and parent directories
        File javaFile = new File(tempDir, "src/Main.java");
        javaFile.getParentFile().mkdirs(); // ensure dirs exist

        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(javaSourceCode);
        }

        // 3. Run Semgrep on the temp dir
        JsonNode semgrepJson = runSemgrep(tempDir.getAbsolutePath());

        // 4. Parse results / log
        List<Issue> issues = parseSemgrepResults(semgrepJson);

        // 5. Clean up
        deleteTempDirectory(tempDir);

        return semgrepJson.toPrettyString();

    }

    private JsonNode runSemgrep(String directoryPath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("semgrep",
                "--config", "auto",
                "--json",
                "--quiet",
                "--no-git-ignore",
                directoryPath);
        pb.redirectErrorStream(true);
        Process process= pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining("\n"));

            // Try parsing clean JSON from the output
            try {
                return objectMapper.readTree(output);
            } catch (JsonParseException e) {
                throw new IOException("Failed to extract JSON from Semgrep output:\n" + output, e);
            }
        }

    }

    private List<Issue> parseSemgrepResults(JsonNode semgrepJson) throws JsonProcessingException {
        List<Issue> issues = new ArrayList<>();
        JsonNode results = semgrepJson.get("results");
        if (results == null || !results.isArray()) return issues;

        for (JsonNode r : results) {
            Issue gi = new Issue();

            JsonNode extra = r.path("extra");
            JsonNode metadata = extra.path("metadata");
            gi.setIssueType(toTitleCase(metadata.path("category").asText(null)));
            gi.setRuleId(r.path("check_id").asText(null));
            gi.setFilePath(r.path("path").asText(null));
            gi.setLine(r.path("start").path("line").asInt(-1));
            gi.setCodeSnippet(extra.path("lines").asText(null));
            gi.setMessage(extra.path("message").asText(null));
            gi.setSeverity(Optional.ofNullable(metadata.path("severity").asText(null)).orElse("UNKNOWN"));
            gi.setRemediation(generateRemediation(gi.getMessage()));
            gi.setReferences(new ArrayList<>());
            metadata.path("references").forEach(ref -> gi.getReferences().add(ref.asText()));
            gi.setTags(new ArrayList<>());
            if (metadata.has("category")) gi.getTags().add(metadata.get("category").asText());
            if (metadata.has("cwe")) metadata.get("cwe").forEach(cwe -> gi.getTags().add(cwe.asText()));

            issues.add(gi);
        }

        return issues;
    }


    private String toTitleCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String generateRemediation(String message) {
        if (message == null) return null;
        if (message.toLowerCase().contains("aes")) {
            return "Use AES/CBC/PKCS7PADDING instead of default AES mode.";
        }
        return "Review the issue and apply best practices.";
    }


    private void deleteTempDirectory(File tempDir) {
        File[] allContents= tempDir.listFiles();
        if(allContents !=  null ){
            for(File f : allContents){
                deleteTempDirectory(f);
            }
        }
        tempDir.delete();
    }

     */

    //--------------------------------------------------//


    public String getSemgrepRuleSchema(){
        return null;
        //TODO
    }

    public List<String> getSupportLanguages() throws McpError {
        List<String> args = Arrays.asList("show", "supported-languages", "--experimental");
        String output = runSemgrep(args, semgrepExecutable, semgrepLock);

        return Arrays.stream(output.split("\n"))
                .map(String::trim)
                .filter(lang -> !lang.isEmpty())
                .collect(Collectors.toList());
    }


    //TODO: use generics (<T>) instead of Object (root of hierarchy)

    public SemgrepScanResult semgrepScan(Map<String, Object> input){
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
            String output= runSemgrep(args, semgrepExecutable, semgrepLock);

            SemgrepScanResult results= objectMapper.readValue(output, SemgrepScanResult.class);
            removeTempDirFromResults(results, tempDir);

            return results;
        } catch (McpError | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: use generics (<T>) instead of Object (root of hierarchy)
    public Object semgrepScanWithCustomeRule(Map<String, Object> input){
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
            String output = runSemgrep(args, semgrepExecutable, semgrepLock);

            SemgrepScanResult results = objectMapper.readValue(output, SemgrepScanResult.class);
            removeTempDirFromResults(results, tempDir);

            return results;
        } catch (McpError e) {
            return Map.of("error", e.getCode(), "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "INTERNAL_ERROR", "message", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    public Object securityCheck(Map<String, Object> input){
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
            String output = runSemgrep(args, semgrepExecutable, semgrepLock);

            SemgrepScanResult results = objectMapper.readValue(output, SemgrepScanResult.class);
            removeTempDirFromResults(results, tempDir);

            if (!results.getResults().isEmpty()) {
                String message = results.getResults().size() + " security issues found in the code.\n\n" +
                        "Here are the details of the security issues found:\n\n" +
                        "<security-issues>\n" +
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results) +
                        "\n</security-issues>";
                return Map.of("message", message);
            } else {
                return Map.of("message", "No security issues found in the code!");
            }
        } catch (McpError e) {
            return Map.of("error", e.getCode(), "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "INTERNAL_ERROR", "message", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    //TODO i can use a generic typing here
    public Object getAbstractSyntaxTree(CodeWithLanguage input) {
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

            return runSemgrep(args, semgrepExecutable, semgrepLock);
        } catch (McpError e) {
            return Map.of("error", e.getCode(), "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "INTERNAL_ERROR", "message", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

}
