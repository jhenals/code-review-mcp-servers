package dev.jhenals.mcp_semgrep_server.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcp_semgrep_server.models.CodeFile;
import dev.jhenals.mcp_semgrep_server.models.SemgrepScanResult;
import org.springframework.boot.json.JsonParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class SemgrepUtils {

    private static String safeJoin(String baseDir, String untrustedPath) throws McpError{
        try{
            Path basePath= Paths.get(baseDir).toRealPath();

            if(untrustedPath == null || untrustedPath.trim().isEmpty() ||
            untrustedPath.equals(".") || untrustedPath.replaceAll("/", "").isEmpty()){
                return basePath.toString();
            }

            if(Paths.get(untrustedPath).isAbsolute()){
                throw new McpError("INVALID_PARAMS", "Untrusted path must be relative");
            }

            Path fullPath= basePath.resolve(untrustedPath).normalize();

            if (!fullPath.startsWith(basePath)) {
                throw new McpError("INVALID_PARAMS", "Untrusted path escapes the base directory: "+ untrustedPath);
            }

            return fullPath.toString();
        } catch (IOException e) {
            throw new McpError("INTERNAL_ERROR", "Failed to solve path: "+ e.getMessage());
        }
    }

    public static String validateAbsolutePath(String pathToValidate, String paramName) throws McpError {
        Path path = Paths.get(pathToValidate);

        if(!path.isAbsolute()){
            throw new McpError("INVALID_PARAMS",
                    paramName + " must be an absolute path. Received: "+ pathToValidate);
        }

        try{
            Path normalized= path.normalize().toRealPath();
            if(!normalized.equals(path.toRealPath())){
                throw new McpError("INVALID_PARAMS",
                        paramName + " contains invalid path traversal sequences.");
            }
            return normalized.toString();
        } catch (IOException e) {
            throw new McpError("INVALID_PARAMS",
                    "Invalid path "+ pathToValidate+ "-"+ e.getMessage());
        }
    }
    public static String validateConfig(String config) throws McpError{
        if(config == null || config.startsWith("p/") ||
                config.startsWith("r/") || config.equals("auto")){
            return config;
        }
        return validateAbsolutePath(config, "config");
    }

    public static String findSemgrepPath(){
        List<String> commonPaths = Arrays.asList(
                "semgrep",
                "/usr/local/bin/semgrep",
                "/usr/bin/semgrep",
                "/opt/homebrew/bin/semgrep",
                "/opt/semgrep/bin/semgrep",
                "/home/linuxbrew/.linuxbrew/bin/semgrep",
                "/snap/bin/semgrep"
                //TODO change the paths
        );

        String os= System.getProperty("os.name").toLowerCase();
        if(os.contains("win")){
            String appData = System.getenv("APPDATA");
            if(appData != null ){
                commonPaths = new ArrayList<>(commonPaths);
                commonPaths.add(Paths.get(appData, "Python", "Scripts", "semgrep.exe").toString());
                commonPaths.add(Paths.get(appData, "npm", "semgrep.cmd").toString());

            }
        }

        for( String semgrepPath : commonPaths ){
            try{
                ProcessBuilder pb = new ProcessBuilder(semgrepPath, "--version");
                Process process = pb.start();
                int exitCode = process.waitFor();
                if(exitCode == 0 )
                    return semgrepPath;
            }catch(Exception e ){
                //Continue to next path
            }
        }
        return null;
    }

    public static String ensureSemgrepAvailable(String semgrepExecutable, ReentrantLock semgrepLock) throws McpError{
        if(semgrepExecutable != null)
            return semgrepExecutable;

        semgrepLock.lock();
        try{
            if(semgrepExecutable != null)
                return semgrepExecutable;

            String semgrepPath = findSemgrepPath();
            if(semgrepPath == null) {
                throw new McpError("INTERNAL_ERROR",
                        "Semgrep is not installed or not in your PATH. " +
                                "Please install Semgrep manually before using this tool. " +
                                "Installation options: pip install semgrep, " +
                                "macOS: brew install semgrep, " +
                                "Or refer to https://semgrep.dev/docs/getting-started/");
            }
            semgrepExecutable = semgrepPath;
            return semgrepPath;
        }finally {
            semgrepLock.unlock();
        }
    }

    public static String createTempFilesFromCodeContent(List<CodeFile> codeFiles) throws McpError{
        try {
            File tempDir = Files.createTempDirectory("semgrep_scan_").toFile();

            for (CodeFile fileInfo : codeFiles) {
                if (fileInfo.getFilename() == null || fileInfo.getFilename().isEmpty()) {
                    continue;
                }else{
                      File javaFile = new File(tempDir, fileInfo.getFilename());
                      javaFile.getParentFile().mkdirs(); // ensure dirs exist

                    try (FileWriter writer = new FileWriter(javaFile)) {
                        writer.write(fileInfo.getContent());
                    } catch (IOException ex) {
                        throw new McpError("INTERNAL_ERROR", "Failed to write file: " + javaFile.getAbsolutePath());
                    }
                }
            }
        return tempDir.getAbsolutePath();
        } catch (IOException e) {
            throw new McpError("INTERNAL_ERROR",
                    "Failed to create temporary files: " + e.getMessage());
        }
    }

    public static List<String> getSemgrepScanArgs(String tempDir, String config) {
        List<String> args = new ArrayList<>(Arrays.asList("semgrep",
                "--json",
                "--quiet",
                "--no-git-ignore"));
        args.add("--config");
        args.add(config);
        args.add(tempDir);
        return args;
    }

    public static void validateCodeFiles(List<CodeFile> codeFiles) throws McpError {
        if (codeFiles == null || codeFiles.isEmpty()) {
            throw new McpError("INVALID_PARAMS", "code_files must be a non-empty list of file objects");
        }

        for (CodeFile file : codeFiles) {
            if (file.getFilename() == null || file.getContent() == null) {

                throw new McpError("INVALID_PARAMS", "Each code file must have filename and content");
            }
            if (Paths.get(file.getFilename()).isAbsolute()) {
                throw new McpError("INVALID_PARAMS","code_files.filename must be a relative path");
            }
        }
    }

    public static String runSemgrepDefault(String directoryPath) throws IOException {
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
                ObjectMapper objectMapper= new ObjectMapper();
                return objectMapper.readTree(output).toPrettyString();
            } catch (JsonParseException e) {
                throw new IOException("Failed to extract JSON from Semgrep output:\n" + output, e);
            }
        }
    }

    public static String runSemgrep(List<String> args, String semgrepExecutable, ReentrantLock semgrepLock) throws McpError {
        try {
            String semgrepPath = ensureSemgrepAvailable(semgrepExecutable, semgrepLock);
            List<String> command = new ArrayList<>();
            command.add(semgrepPath);
            command.addAll(args);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            try (BufferedReader stdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                 BufferedReader stderrReader = new BufferedReader(
                         new InputStreamReader(process.getErrorStream()))) {

                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
                while ((line = stderrReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("[DEBUG] Process exited with code: " + exitCode);

            if (exitCode != 0) {
                throw new McpError("INTERNAL_ERROR",
                        "Error running semgrep: (" + exitCode + ") " + stderr.toString());
            }

            return stdout.toString();
        } catch (IOException | InterruptedException e) {
            throw new McpError("INTERNAL_ERROR",
                    "Failed to run semgrep: " + e.getMessage());
        }
    }


    public static void removeTempDirFromResults(SemgrepScanResult results, String tempDir) {
        Path tempPath = Paths.get(tempDir);

        // Process findings results
        for (Map<String, Object> finding : results.getResults()) {
            if (finding.containsKey("path")) {
                String path = (String) finding.get("path");
                try {
                    String relativePath = tempPath.relativize(Paths.get(path)).toString();
                    finding.put("path", relativePath);
                } catch (Exception e) {
                    // Skip if path is not relative to temp_dir
                }
            }
        }

        // Process scanned paths
        Map<String, Object> paths = results.getPaths();
        if (paths.containsKey("scanned")) {
            @SuppressWarnings("unchecked")
            List<String> scannedPaths = (List<String>) paths.get("scanned");
            if (scannedPaths != null) {
                scannedPaths.replaceAll(path -> {
                    try {
                        return tempPath.relativize(Paths.get(path)).toString();
                    } catch (Exception e) {
                        return path;
                    }
                });
            }
        }

        if (paths.containsKey("skipped")) {
            @SuppressWarnings("unchecked")
            List<String> skippedPaths = (List<String>) paths.get("skipped");
            if (skippedPaths != null) {
                skippedPaths.replaceAll(path -> {
                    try {
                        return tempPath.relativize(Paths.get(path)).toString();
                    } catch (Exception e) {
                        return path;
                    }
                });
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


}
