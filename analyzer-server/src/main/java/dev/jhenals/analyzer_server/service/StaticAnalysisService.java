package dev.jhenals.analyzer_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.analyzer_server.models.StaticAnalysisResult;
import dev.jhenals.analyzer_server.models.Commit;
import dev.jhenals.analyzer_server.models.PRInput;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaticAnalysisService {
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        String semgrepJson = runSemgrep(tempDir.getAbsolutePath()).toPrettyString();

        // 4. Optional: parse results / log
        // List<String> issues = parseSemgrepResults(semgrepJson);

        // 5. Clean up
        deleteTempDirectory(tempDir);

        return semgrepJson;

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

    private List<String> parseSemgrepResults(String semgrepJson) throws JsonProcessingException {
        List<String> issues = new ArrayList<>();
        JsonNode root = objectMapper.readTree(semgrepJson);
        JsonNode results = root.get("results");

        if (results == null || !results.isArray() || results.size() == 0) {
            return issues; // no issues
        }

        for (JsonNode issue : results) {
            String checkId = issue.path("check_id").asText("N/A");
            String path = issue.path("path").asText("unknown");
            int line = issue.path("start").path("line").asInt(-1);
            String message = issue.path("extra").path("message").asText("No message");

            issues.add(String.format("[Semgrep][%s] %s:%d - %s", checkId, path, line, message));
        }

        return issues;
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
