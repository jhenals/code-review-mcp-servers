package dev.jhenals.mcpsemgrep.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessUtils {

    public static final int MAX_OUTPUT_LINES = 10000;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_MINUTES= 10;
    //private static final int MAX_OUTPUT_LINES = 10000; //Preventmemory issues wiht large outputs


    public JsonNode executeCommand(List<String> command) throws McpAnalysisException, IOException {
        return executeCommand(command, DEFAULT_TIMEOUT_MINUTES);
    }

    public JsonNode executeCommand(List<String> command, int timeOutInMinutes)throws McpAnalysisException, IOException{
        log.info("Executing command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try{
            process= processBuilder.start();
            String output = captureProcessOutput(process);
            boolean finished = process.waitFor(timeOutInMinutes, TimeUnit.MINUTES);
            if(!finished){
                process.destroyForcibly();
                throw new McpAnalysisException("PROCESS_TIMEOUT",
                        String.format("Command timed out after %d minutes: %s", timeOutInMinutes, String.join(" ", command)));
            }

            int exitCode = process.exitValue();
            log.debug("Command failed with exit code %d: %s", exitCode, output);

            if(exitCode!=0){
                log.warn("Command failed with exit code {}: {}", exitCode, output);
                throw  new McpAnalysisException("PROCESS_EXECUTION_FAILED",
                        String.format("Command failed with exit code %d: %s", exitCode, output));
            }

            return parseJsonOutput(output);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new McpAnalysisException("PROCESS_INTERRUPTED",
                    "Command execution was interrupted: " + String.join(" ", command));
        }finally {
            if(process!=null && process.isAlive()){
                process.destroyForcibly();
            }
        }
    }


    public String executeCommandForString(List<String> command)throws McpAnalysisException, IOException{
        return executeCommandForString(command, DEFAULT_TIMEOUT_MINUTES);
    }

    public String executeCommandForString(List<String> command, int timeoutMinutes) throws McpAnalysisException, IOException{
        log.info("Executing command for string output: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process= null;
        try{
            process = processBuilder.start();

            String output = captureProcessOutput(process);

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);

            if(!finished){
                process.destroyForcibly();
                throw new McpAnalysisException("PROCESS_TIMEOUT",
                        String.format("Command timed out after %d minutes: %s", timeoutMinutes, String.join(" ", command)));
            }

            int exitCode= process.exitValue();

            if(exitCode != 0){
                log.warn("Command failed with exit code {}: {}", exitCode, output);
                throw new McpAnalysisException("PROCESS_EXECUTION_FAILED",
                        String.format("Command failed with exit code %d: %s", exitCode, output));
            }

            return output;
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new McpAnalysisException("PROCESS_INTERRUPTED",
                    "Command execution was interrupted: "+ String.join(" ", command));
        }finally {
            if(process != null && process.isAlive()){
                process.destroyForcibly();
            }
        }
    }

    public boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder processBuilder;
            if (isWindows()) {
                processBuilder = new ProcessBuilder("where", command);
            } else {
                processBuilder = new ProcessBuilder("which", command);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            log.debug("Error checking command availability for '{}': {}", command, e.getMessage());
            return false;
        }
    }

    public void validateRequiredCommands(List<String> requiredCommands) throws McpAnalysisException {
        for (String command : requiredCommands) {
            if (!isCommandAvailable(command)) {
                throw new McpAnalysisException("COMMAND_NOT_FOUND",
                        String.format("Required command '%s' is not available in PATH", command));
            }
        }
    }

    public ProcessBuilder createProcessBuilder(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        // Set environment variables if needed
        // processBuilder.environment().put("SEMGREP_SETTINGS_FILE", "/dev/null");

        return processBuilder;
    }

    /**
     * Captures all output from a process.
     *
     * @param process The process to capture output from
     * @return Combined stdout and stderr as a string
     * @throws IOException if reading fails
     */
    private String captureProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null && lineCount < MAX_OUTPUT_LINES) {
                output.append(line).append(System.lineSeparator());
                lineCount++;
            }

            if (lineCount >= MAX_OUTPUT_LINES) {
                log.warn("Process output truncated after {} lines to prevent memory issues", MAX_OUTPUT_LINES);
                output.append("... [Output truncated due to length] ...");
            }
        }

        return output.toString();
    }

    /**
     * Parses JSON output from a command.
     *
     * @param output Raw string output
     * @return JsonNode containing parsed JSON
     * @throws McpAnalysisException if output is not valid JSON
     */
    private JsonNode parseJsonOutput(String output) throws McpAnalysisException {
        if (output == null || output.trim().isEmpty()) {
            throw new McpAnalysisException("EMPTY_OUTPUT", "Command produced no output");
        }

        try {
            return objectMapper.readTree(output.trim());
        } catch (JsonParseException e) {
            log.error("Failed to parse JSON output: {}", output);
            throw new McpAnalysisException("INVALID_JSON_OUTPUT",
                    "Command output is not valid JSON: " + e.getMessage());
        } catch (IOException e) {
            throw new McpAnalysisException("JSON_PARSE_ERROR",
                    "Error parsing JSON output: " + e.getMessage());
        }
    }

    /**
     * Checks if the current operating system is Windows.
     *
     * @return true if running on Windows, false otherwise
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Logs command execution details for debugging.
     *
     * @param command The command being executed
     * @param exitCode The exit code returned
     * @param executionTimeMs Time taken to execute in milliseconds
     */
    public void logExecutionMetrics(List<String> command, int exitCode, long executionTimeMs) {
        log.info("Command execution completed - Command: {}, Exit Code: {}, Duration: {}ms",
                String.join(" ", command), exitCode, executionTimeMs);
    }

    /**
     * Builder class for configuring process execution parameters.
     */
    public static class ProcessExecutionBuilder {
        private List<String> command;
        private int timeoutMinutes = DEFAULT_TIMEOUT_MINUTES;
        private boolean validateCommands = true;

        public ProcessExecutionBuilder command(List<String> command) {
            this.command = command;
            return this;
        }

        public ProcessExecutionBuilder timeout(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
            return this;
        }

        public ProcessExecutionBuilder expectJson(boolean expectJsonOutput) {
            return this;
        }

        public ProcessExecutionBuilder validateCommands(boolean validateCommands) {
            this.validateCommands = validateCommands;
            return this;
        }

        public JsonNode executeForJson(ProcessUtils processUtils) throws McpAnalysisException, IOException {
            if (validateCommands && !command.isEmpty()) {
                processUtils.validateRequiredCommands(List.of(command.get(0)));
            }
            return processUtils.executeCommand(command, timeoutMinutes);
        }

        public String executeForString(ProcessUtils processUtils) throws McpAnalysisException, IOException {
            if (validateCommands && !command.isEmpty()) {
                processUtils.validateRequiredCommands(List.of(command.get(0)));
            }
            return processUtils.executeCommandForString(command, timeoutMinutes);
        }
    }

    /**
     * Creates a builder for configuring process execution.
     *
     * @return ProcessExecutionBuilder instance
     */
    public ProcessExecutionBuilder builder() {
        return new ProcessExecutionBuilder();
    }
}

/*

    public static JsonNode runSemgrepService(ArrayList<String> commands, String absolutePath) throws IOException, McpAnalysisException {

        commands.add(absolutePath);

//        ProcessBuilder pb = new ProcessBuilder(commands);
//        pb.redirectErrorStream(true);
//        Process process = pb.start();
//
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            String output = reader.lines().collect(Collectors.joining("\n"));
//
//            try {
//                ObjectMapper objectMapper = new ObjectMapper();
//                return objectMapper.readTree(output);
//            } catch (JsonParseException e) {
//                throw new IOException("Failed to extract JSON from Semgrep output:\n" + output, e);
//            }
//        }

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
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
            if (exitCode != 0) {
                throw new McpAnalysisException("INTERNAL_ERROR",
                        "Error running mcpsemgrep: (" + exitCode + ") " + stderr);
            }

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(String.valueOf(stdout));
            } catch (JsonParseException e) {
                throw new IOException("Failed to extract JSON from Semgrep output:\n" + stdout, e);
            }
        } catch (IOException | InterruptedException e) {
            throw new McpAnalysisException("INTERNAL_ERROR",
                    "Failed to run mcpsemgrep: " + e.getMessage());
        } catch (McpAnalysisException e) {
            throw new RuntimeException(e);
        }
    }



    public static String validateAbsolutePath(String pathToValidate, String paramName) throws McpAnalysisException {
        Path path = Paths.get(pathToValidate);

        if (!path.isAbsolute()) {
            log.info("Must be an absolute path. Received {}", pathToValidate);
            throw new McpAnalysisException("INVALID_PARAMS",
                    paramName + " must be an absolute path. Received: " + pathToValidate);
        }

        try {
            Path normalized = path.normalize().toRealPath();
            if (!normalized.equals(path.toRealPath())) {
                throw new McpAnalysisException("INVALID_PARAMS",
                        paramName + " contains invalid path traversal sequences.");
            }
            log.info("Normalized path: {}", normalized);
            return normalized.toString();
        } catch (IOException e) {
            throw new McpAnalysisException("INVALID_PARAMS",
                    "Invalid path " + pathToValidate + "-" + e.getMessage());
        }
    }






//
//
//    public static void removeTempDirFromResults(AnalysisResult results, String tempDir) {
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

 */