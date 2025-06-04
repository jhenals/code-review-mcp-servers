package dev.jhenals.analyzer_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.analyzer_server.models.StaticAnalysisResult;
import dev.jhenals.analyzer_server.models.Commit;
import dev.jhenals.analyzer_server.models.PRInput;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaticAnalysisService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StaticAnalysisResult analyzeCode(PRInput input) throws IOException {
        StaticAnalysisResult result= new StaticAnalysisResult();
        int numIssues=0;

        //1. Save diffs from commit as files in temp dir for SemgrepAnalysis
        File tempDir = Files.createTempDirectory("pr-diffs").toFile();
        for( Commit commit: input.getCommits() ){
            String safeFileName = "commit_"+ commit.message.hashCode()+".diff";
            File diffFile= new File(tempDir, safeFileName);
            try(FileWriter fw= new FileWriter(diffFile)){
                fw.write(commit.diff);
            }
        }

        //2. Run Semgrep on the temp dir
        String semgrepJson= runSemgrep(tempDir.getAbsolutePath());

        //3. Parse Semgrep results and add to issues list
        List<String> semgrepIssues=  parseSemgrepResults(semgrepJson);
        numIssues += semgrepIssues.size();
        result.issues.addAll(semgrepIssues);

        //4. Clean up temp dir
        deleteTempDirectory(tempDir);
        return result;

    }

    private String runSemgrep(String directoryPath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("semgrep", "--config", "auto", directoryPath, "--json");
        pb.redirectErrorStream(true);
        Process process= pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Semgrep failed with exit code " + exitCode + ". Output:\n" + output);
            }
            return output;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
