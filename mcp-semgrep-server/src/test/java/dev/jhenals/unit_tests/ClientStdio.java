package dev.jhenals.unit_tests;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
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

    public static void main(String[] args) throws IOException {
        ServerParameters  stdioParams = ServerParameters.builder("java")
			.args("-Dspring.ai.mcp.server.transport=STDIO",
                    "-jar",
					"C:\\.My_Projects\\mcp\\AI-Code-Review-Assistant\\mcp-semgrep-server\\target\\mcp-semgrep-server-0.0.1-SNAPSHOT.jar")			.build();

		var transport = new StdioClientTransport(stdioParams);
        var client = McpClient.sync(transport).build();

        var result = client.initialize();
        log.info("CLIENT initialized: {}", result);

		// List and demonstrate tools
		McpSchema.ListToolsResult toolsList = client.listTools();
        log.info("AVAILABLE TOOLS = {}", toolsList);


    }


}
