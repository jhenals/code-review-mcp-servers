package dev.jhenals.unit_tests;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * With stdio transport, the MCP server is automatically started by the client. But you
 * have to build the server jar first:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 */

public class ClientStdio {
    private static final Logger log = LoggerFactory.getLogger(ClientStdio.class);

    public static void main(String[] args) {
        ServerParameters  stdioParams = ServerParameters.builder("java")
			.args("-Dspring.ai.mcp.server.transport=STDIO",
                    "-jar",
					"C:\\.My_Projects\\mcp\\AI-Code-Review-Assistant\\mcp-semgrep-server\\target\\mcp-semgrep-server-0.0.1-SNAPSHOT.jar")			.build();

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

		// List and demonstrate tools
		McpSchema.ListToolsResult toolsList = client.listTools();
        log.info("AVAILABLE TOOLS = {}", toolsList);

        log.info("-------TOOL TESTING-----------------------------------------");

        log.info("Tool 1: Semgrep Scan----------------------------------------");
        Map<String, Object> semgrepInput= new HashMap<>();
        semgrepInput.put("config", "auto");
        Map<String, String> codeFile= new HashMap<>();
        codeFile.put("filename", "Example.java");
        codeFile.put("content", "public class Example { public void doSomething() { System.out.println(\"Hello\"); } }");
        semgrepInput.put("code_file", codeFile);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("semgrep_scan", Map.of("input", semgrepInput));
        log.info("Input to semgrepScan tool: {}", request.arguments());

        McpSchema.CallToolResult semgrepScanResult = client.callTool(request);

        log.info("Semgrep scan result: {}", semgrepScanResult);

        client.closeGracefully();

    }


}
