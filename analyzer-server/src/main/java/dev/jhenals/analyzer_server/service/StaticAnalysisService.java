package dev.jhenals.analyzer_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.analyzer_server.models.Issue;
import dev.jhenals.analyzer_server.models.StaticAnalysisResult;
import dev.jhenals.analyzer_server.models.Commit;
import dev.jhenals.analyzer_server.models.PRInput;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StaticAnalysisService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Issue> analyzeCode(String javaSourceCode) throws IOException {
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

        return issues;

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
            gi.issueType = toTitleCase(metadata.path("category").asText(null));
            gi.ruleId = r.path("check_id").asText(null);
            gi.filePath = r.path("path").asText(null);
            gi.line = r.path("start").path("line").asInt(-1);
            gi.codeSnippet = extra.path("lines").asText(null);
            gi.message = extra.path("message").asText(null);
            gi.severity = Optional.ofNullable(metadata.path("severity").asText(null)).orElse("UNKNOWN");
            gi.remediation = generateRemediation(gi.message);
            gi.references = new ArrayList<>();
            metadata.path("references").forEach(ref -> gi.references.add(ref.asText()));
            gi.tags = new ArrayList<>();
            if (metadata.has("category")) gi.tags.add(metadata.get("category").asText());
            if (metadata.has("cwe")) metadata.get("cwe").forEach(cwe -> gi.tags.add(cwe.asText()));

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

}
