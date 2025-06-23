package dev.jhenals.mcpsemgrep.integration;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 *McpClientDocker demonstrates how to start and communicate with the MCP server
 * using a Docker container as the transport mechanism.
 * <p>
 * The MCP server is run inside a Docker container, which is automatically started
 * by the client. This requires Docker to be installed and the image
 * <code>jhena/mcp-mcpsemgrep-server:latest</code> to be available locally or remotely.
 * </p>
 *
 * <p>
 * The client initializes the connection, lists available controller, and demonstrates
 * calling the "semgrep_scan" controller with example Java code.
 * </p>
 *
 * <p>
 * Usage:
 * Make sure Docker engine is running
 * <pre>
 *   java dev.jhenals.mcpsemgreprep.unit_tests.McpClientDocker
 * </pre>
 * </p>
 */

public class McpClientDocker {
    private static final Logger log = LoggerFactory.getLogger(McpClientDocker.class);

    public static void main(String[] args) {
        ServerParameters  stdioParams = ServerParameters.builder("docker")
                .args("run", "-i", "--rm", "jhena/mcp-mcpsemgrep-server:latest")
                .build();

        var transport = new StdioClientTransport(stdioParams);
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofMinutes(10))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)      // Enable roots capability
                        .sampling()       // Enable sampling capability
                        .build())
                .build();

        var result = client.initialize();
        log.info("CLIENT initialized: {}", result);

        // List and demonstrate controller
        McpSchema.ListToolsResult toolsList = client.listTools();
        log.info("AVAILABLE TOOLS = {}", toolsList);

        log.info("-------TOOL TESTING-----------------------------------------");

        log.info("Tool 1: Semgrep Scan----------------------------------------");
        McpSchema.CallToolRequest request = getCallToolRequest();
        //log.info("Input to semgrepScan controller: {}", request.arguments());

        McpSchema.CallToolResult semgrepScanResult = client.callTool(request);

        log.info("Semgrep scan result: {}", semgrepScanResult);

        client.closeGracefully();

    }

    private static McpSchema.CallToolRequest getCallToolRequest() {
        Map<String, Object> semgrepInput= new HashMap<>();
        semgrepInput.put("McpConfiguration", "auto");
        Map<String, String> codeFile= new HashMap<>();
        codeFile.put("filename", "Example.java");
        codeFile.put("content", """
                public class SemgrepAutoConfigTest {
                            public static void main(String[] args) {
                                // Example of hardcoded password - mcpsemgrep auto McpConfiguration may detect this
                                String password = "password123";
                                // Example of dangerous command execution
                                try {
                                    Runtime.getRuntime().exec("rm -rf /tmp/test");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // Example of printing to console
                                System.out.println("Test complete");
                            }
                        }
                """);
        semgrepInput.put("code_file", codeFile);

        return new McpSchema.CallToolRequest("semgrep_scan", Map.of("input", semgrepInput));
    }

}
