package dev.jhenals.mcp_semgrep_server.utils;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SemgrepUtils {

    public static File createTemporaryFile(CodeFile codefile) throws IOException {
        String tmpdirPath= Files.createTempDirectory("semgrepTempDir").toFile().getAbsolutePath();

        File javaFile = new File(tmpdirPath, codefile.getFileName());
        javaFile.getParentFile().mkdirs();

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(javaFile)))) {
            writer.write(codefile.getContent());
        }
        return javaFile;
    }

    public static JsonNode runSemgrepService(ArrayList<String> commands, String absolutePath) throws IOException {
        commands.add(absolutePath);

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining("\n"));

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(output);
            } catch (JsonParseException e) {
                throw new IOException("Failed to extract JSON from Semgrep output:\n" + output, e);
            }
        }
    }

    public static void cleanupTempDir(String tempDir) {
        if (tempDir != null) {
            try {
                Files.walk(Paths.get(tempDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                System.err.println("Failed to cleanup temp directory: " + e.getMessage());
            }
        }
    }

    public static String validateAbsolutePath(String pathToValidate, String paramName) throws McpError {
        Path path = Paths.get(pathToValidate);

        if (!path.isAbsolute()) {
            log.info("Must be an absolute path. Received {}", pathToValidate);
            throw new McpError("INVALID_PARAMS",
                    paramName + " must be an absolute path. Received: " + pathToValidate);
        }

        try {
            Path normalized = path.normalize().toRealPath();
            if (!normalized.equals(path.toRealPath())) {
                throw new McpError("INVALID_PARAMS",
                        paramName + " contains invalid path traversal sequences.");
            }
            log.info("Normalized path: {}", normalized);
            return normalized.toString();
        } catch (IOException e) {
            throw new McpError("INVALID_PARAMS",
                    "Invalid path " + pathToValidate + "-" + e.getMessage());
        }
    }

    public static String validateConfig(String config) throws McpError {
        if(config == null){
            return "auto";
        }
        else if (config.startsWith("p/") ||
                config.startsWith("r/") || config.equals("auto")) {
            return config;
        }
        return validateAbsolutePath(config, "config");
    }





//
//
//    public static void removeTempDirFromResults(StaticAnalysisResult results, String tempDir) {
//        Path tempPath = Paths.get(tempDir);
//
//        // Process findings results
//        for (Map<String, Object> finding : results.getResults()) {
//            if (finding.containsKey("path")) {
//                String path = (String) finding.get("path");
//                try {
//                    String relativePath = tempPath.relativize(Paths.get(path)).toString();
//                    finding.put("path", relativePath);
//                } catch (Exception e) {
//                    // Skip if path is not relative to temp_dir
//                }
//            }
//        }
//
//        // Process scanned paths
//        Map<String, Object> paths = results.getPaths();
//        if (paths.containsKey("scanned")) {
//            @SuppressWarnings("unchecked")
//            List<String> scannedPaths = (List<String>) paths.get("scanned");
//            if (scannedPaths != null) {
//                scannedPaths.replaceAll(path -> {
//                    try {
//                        return tempPath.relativize(Paths.get(path)).toString();
//                    } catch (Exception e) {
//                        return path;
//                    }
//                });
//            }
//        }
//
//        if (paths.containsKey("skipped")) {
//            @SuppressWarnings("unchecked")
//            List<String> skippedPaths = (List<String>) paths.get("skipped");
//            if (skippedPaths != null) {
//                skippedPaths.replaceAll(path -> {
//                    try {
//                        return tempPath.relativize(Paths.get(path)).toString();
//                    } catch (Exception e) {
//                        return path;
//                    }
//                });
//            }
//        }
//    }

}
