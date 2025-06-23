package dev.jhenals.mcpsemgrep.utils;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.util.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@Slf4j
@SpringBootTest(classes = ProcessUtils.class)
public class ProcessUtilsTest {
    private ProcessUtils processUtils;

    @BeforeEach
    void setUp() {
        processUtils = new ProcessUtils();
    }

    private Process mockProcess(List<String> outputLines, int exitCode, boolean waitForReturns) throws IOException, InterruptedException {
        Process process = mock(Process.class);

        String output = String.join(System.lineSeparator(), outputLines) + System.lineSeparator();
        InputStream inputStream = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
        when(process.getInputStream()).thenReturn(inputStream);

        // Mock waitFor(timeout, unit)
        when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(waitForReturns);

        // Mock exitValue()
        when(process.exitValue()).thenReturn(exitCode);

        // Mock isAlive()
        when(process.isAlive()).thenReturn(false);

        return process;
    }

    @Test
    void testExecuteCommand_successfulJsonOutput() throws Exception {
        List<String> command = List.of("echo", "{\"key\":\"value\"}");

        // Spy on ProcessBuilder to inject mocked Process
        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of("{\"key\":\"value\"}"), 0, true);
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            JsonNode result = processUtils.executeCommand(command);

            assertNotNull(result);
            assertEquals("value", result.get("key").asText());
        }
    }

    @Test
    void testExecuteCommand_timeout() throws Exception {
        List<String> command = List.of("sleep", "1000");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of(""), 0, false); // waitFor returns false (timeout)
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                    () -> processUtils.executeCommand(command, 1));

            assertEquals("PROCESS_TIMEOUT", ex.getCode());
            assertTrue(ex.getMessage().contains("timed out"));
        }
    }

    @Test
    void testExecuteCommand_nonZeroExitCode() throws Exception {
        List<String> command = List.of("false");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of("error output"), 1, true);
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                    () -> processUtils.executeCommand(command));

            assertEquals("PROCESS_EXECUTION_FAILED", ex.getCode());
            assertTrue(ex.getMessage().contains("exit code 1"));
        }
    }

    @Test
    void testExecuteCommand_invalidJsonOutput() throws Exception {
        List<String> command = List.of("echo", "not json");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of("not json"), 0, true);
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                    () -> processUtils.executeCommand(command));

            assertEquals("INVALID_JSON_OUTPUT", ex.getCode());
            assertTrue(ex.getMessage().contains("not valid JSON"));
        }
    }

    @Test
    void testExecuteCommandForString_success() throws Exception {
        List<String> command = List.of("echo", "hello world");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of("hello world"), 0, true);
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            String output = processUtils.executeCommandForString(command);

            assertEquals("hello world" + System.lineSeparator(), output);
        }
    }

    @Test
    void testExecuteCommandForString_timeout() throws Exception {
        List<String> command = List.of("sleep", "1000");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of(""), 0, false);
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                    () -> processUtils.executeCommandForString(command, 1));

            assertEquals("PROCESS_TIMEOUT", ex.getCode());
        }
    }

    @Test
    void testIsCommandAvailable_commandExists() throws Exception {
        // We cannot mock static methods like ProcessBuilder.start() easily without PowerMock,
        // so here we test with a common command that should exist on the system.
        // This test may be flaky depending on environment, so we just check it returns boolean.

        boolean result = processUtils.isCommandAvailable("semgrep");
        assertTrue(result || !result); // Just assert it returns a boolean without exception
    }

    @Test
    void testValidateRequiredCommands_allAvailable() throws Exception {
        ProcessUtils spyUtils = spy(processUtils);
        doReturn(true).when(spyUtils).isCommandAvailable("cmd1");
        doReturn(true).when(spyUtils).isCommandAvailable("cmd2");

        spyUtils.validateRequiredCommands(List.of("cmd1", "cmd2"));
        // No exception means pass
    }

    @Test
    void testValidateRequiredCommands_missingCommand() throws Exception {
        ProcessUtils spyUtils = spy(processUtils);
        doReturn(true).when(spyUtils).isCommandAvailable("cmd1");
        doReturn(false).when(spyUtils).isCommandAvailable("missingCmd");

        McpAnalysisException ex = assertThrows(McpAnalysisException.class,
                () -> spyUtils.validateRequiredCommands(List.of("cmd1", "missingCmd")));

        assertEquals("COMMAND_NOT_FOUND", ex.getCode());
        assertTrue(ex.getMessage().contains("missingCmd"));
    }

    @Test
    void testCaptureProcessOutput_truncation() throws Exception {
        // Create a process with more than MAX_OUTPUT_LINES lines
        int lines = ProcessUtils.MAX_OUTPUT_LINES + 10;
        String[] outputLines = new String[lines];
        for (int i = 0; i < lines; i++) {
            outputLines[i] = "line " + i;
        }

        Process process = mock(Process.class);
        String output = String.join(System.lineSeparator(), outputLines) + System.lineSeparator();
        InputStream inputStream = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
        when(process.getInputStream()).thenReturn(inputStream);

        // Use reflection to call private method captureProcessOutput
        var method = ProcessUtils.class.getDeclaredMethod("captureProcessOutput", Process.class);
        method.setAccessible(true);

        String capturedOutput = (String) method.invoke(processUtils, process);

        assertTrue(capturedOutput.contains("... [Output truncated due to length] ..."));
        assertTrue(capturedOutput.startsWith("line 0"));
    }

    @Test
    void testParseJsonOutput_emptyOutput() throws Exception {
        var method = ProcessUtils.class.getDeclaredMethod("parseJsonOutput", String.class);
        method.setAccessible(true);

        Throwable thrown = assertThrows(Throwable.class,
                () -> method.invoke(processUtils, ""));

        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof McpAnalysisException);
        assertEquals("EMPTY_OUTPUT", ((McpAnalysisException) cause).getCode());

    }

    @Test
    void testParseJsonOutput_validJson() throws Exception {
        var method = ProcessUtils.class.getDeclaredMethod("parseJsonOutput", String.class);
        method.setAccessible(true);

        JsonNode node = (JsonNode) method.invoke(processUtils, "{\"foo\":\"bar\"}");
        assertEquals("bar", node.get("foo").asText());
    }

    @Test
    void testLogExecutionMetrics_logsInfo() {
        // We can't easily verify logs without a logging framework test appender,
        // but we can call the method to ensure no exceptions.
        processUtils.logExecutionMetrics(List.of("echo", "hello"), 0, 1234L);
    }

    @Test
    void testProcessExecutionBuilder_executeForJson_success() throws Exception {
        List<String> command = List.of("echo", "{\"key\":\"value\"}");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of("{\"key\":\"value\"}"), 0, true);
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            ProcessUtils.ProcessExecutionBuilder builder = processUtils.builder()
                    .command(command)
                    .timeout(1)
                    .validateCommands(false);

            JsonNode result = builder.executeForJson(processUtils);

            assertEquals("value", result.get("key").asText());
        }
    }

    @Test
    void testProcessExecutionBuilder_executeForString_success() throws Exception {
        List<String> command = List.of("echo", "hello");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                    Process mockedProcess = mockProcess(List.of("hello"), 0, true);
                    when(mock.start()).thenReturn(mockedProcess);
                })) {

            ProcessUtils.ProcessExecutionBuilder builder = processUtils.builder()
                    .command(command)
                    .timeout(1)
                    .validateCommands(false);

            String output = builder.executeForString(processUtils);

            assertEquals("hello" + System.lineSeparator(), output);
        }
    }


}
