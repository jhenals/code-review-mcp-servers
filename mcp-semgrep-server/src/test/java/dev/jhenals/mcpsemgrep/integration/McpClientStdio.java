package dev.jhenals.mcpsemgrep.integration;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public class McpClientStdio {
    public static void main(String[] args) {
        String jarPath=
                "C:\\.My_Projects\\mcp\\AI-Code-Review-Assistant\\mcp-semgrep-server\\target\\mcp-semgrep-server-0.0.1-SNAPSHOT.jar";
        ServerParameters  stdioParams = ServerParameters.builder("java")
			.args("-Dspring.ai.mcp.server.transport=STDIO",
                    "-jar",
                    jarPath)
                .build();

		var transport = new StdioClientTransport(stdioParams);
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofMinutes(10))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)      // Enable roots capability
                        .sampling()       // Enable sampling capability
                        .build())
                .build();

        try {
            // Initialize the client
            var result = client.initialize();
            log.info("[CLIENT INITIALIZED]: {}", result);

            // List available tools
            McpSchema.ListToolsResult toolsList = client.listTools();
            log.info("[AVAILABLE TOOLS]: {}", toolsList);

            log.info("=".repeat(80));
            log.info("TESTING MCP SEMGREP TOOLS");
            log.info("=".repeat(80));

            // Test 1: Basic Semgrep Scan
            testSemgrepScan(client);

            // Test 2: Security Check
            testSecurityCheck(client);

            // Test 3: Custom Rule Scan
            testCustomRuleScan(client);

        } catch (Exception e) {
            log.error("Error during MCP client testing", e);
        } finally {
            client.closeGracefully();
        }
    }


    /**
     * Test the semgrep_scan tool with auto configuration
     */
    private static void testSemgrepScan(McpSyncClient client) {
        log.info("\n--- Testing semgrep_scan tool ---");

        try {
            McpSchema.CallToolRequest request = createSemgrepScanRequest();
            McpSchema.CallToolResult result = client.callTool(request);

            log.info("✅ Semgrep scan completed successfully");
            log.info("Result: {}", result);
        } catch (Exception e) {
            log.error("❌ Semgrep scan failed", e);
        }
    }

    /**
     * Test the security_check tool
     */
    private static void testSecurityCheck(McpSyncClient client) {
        log.info("\n--- Testing security_check tool ---");

        try {
            McpSchema.CallToolRequest request = createSecurityCheckRequest();
            McpSchema.CallToolResult result = client.callTool(request);

            log.info("✅ Security check completed successfully");
            log.info("Result: {}", result);
        } catch (Exception e) {
            log.error("❌ Security check failed", e);
        }
    }

    /**
     * Test the semgrep_scan_with_custom_rule tool
     */
    private static void testCustomRuleScan(McpSyncClient client) {
        log.info("\n--- Testing semgrep_scan_with_custom_rule tool ---");

        try {
            McpSchema.CallToolRequest request = createCustomRuleScanRequest();
            McpSchema.CallToolResult result = client.callTool(request);

            log.info("✅ Custom rule scan completed successfully");
            log.info("Result: {}", result);
        } catch (Exception e) {
            log.error("❌ Custom rule scan failed", e);
        }
    }

    /**
     * Creates request for semgrep_scan tool
     */
    private static McpSchema.CallToolRequest createSemgrepScanRequest() {
        Map<String, Object> arguments = new HashMap<>();

        // Create code file map - note: use "fileName" not "filename"
        Map<String, String> codeFile = new HashMap<>();
        codeFile.put("fileName", "VulnerableExample.java");
        codeFile.put("content", getTestJavaCode());

        // Set the arguments as expected by CodeAnalysisRequest
        arguments.put("codeFile", codeFile);
        arguments.put("config", "auto");

        return new McpSchema.CallToolRequest("semgrep_scan", arguments);
    }

    /**
     * Creates request for security_check tool
     */
    private static McpSchema.CallToolRequest createSecurityCheckRequest() {
        Map<String, Object> arguments = new HashMap<>();

        Map<String, String> codeFile = new HashMap<>();
        codeFile.put("fileName", "SecurityTest.java");
        codeFile.put("content", getSecurityTestCode());

        arguments.put("codeFile", codeFile);
        arguments.put("config", "auto"); // Optional - will default to auto if not provided

        return new McpSchema.CallToolRequest("security_check", arguments);
    }

    /**
     * Creates request for semgrep_scan_with_custom_rule tool
     */
    private static McpSchema.CallToolRequest createCustomRuleScanRequest() {
        Map<String, Object> arguments = new HashMap<>();

        Map<String, String> codeFile = new HashMap<>();
        codeFile.put("fileName", "CustomRuleTest.java");
        codeFile.put("content", getCustomRuleTestCode());

        // Custom YAML rule to detect hardcoded passwords
        String customRule = """
                rules:
                  - id: hardcoded-password
                    patterns:
                      - pattern: String $PASSWORD = "$SECRET"
                    message: "Hardcoded password detected"
                    severity: ERROR
                    languages: [java]
                    metadata:
                      category: security
                      
                  - id: dangerous-exec
                    patterns:
                      - pattern: Runtime.getRuntime().exec($CMD)
                    message: "Dangerous command execution detected"
                    severity: ERROR
                    languages: [java]
                    metadata:
                      category: security
                """;

        arguments.put("codeFile", codeFile);
        arguments.put("customRule", customRule);

        return new McpSchema.CallToolRequest("semgrep_scan_with_custom_rule", arguments);
    }

    /**
     * Sample Java code with various vulnerabilities for testing
     */
    private static String getTestJavaCode() {
        return """
                public class VulnerableExample {
                    public static void main(String[] args) {
                        // Hardcoded password - should be detected
                        String password = "password123";
                        
                        // Dangerous command execution - should be detected
                        try {
                            Runtime.getRuntime().exec("rm -rf /tmp/test");
                        } catch (Exception e) {
                            e.printStackTrace(); // Debug code - should be detected
                        }
                        
                        // Console output
                        System.out.println("Test complete with password: " + password);
                    }
                }
                """;
    }

    /**
     * Code specifically for security testing
     */
    private static String getSecurityTestCode() {
        return """
                import java.sql.*;
                import javax.crypto.Cipher;
                
                public class SecurityTest {
                    private static final String DB_PASSWORD = "admin123"; // Hardcoded credential
                    
                    public void sqlInjectionVulnerable(String userInput) throws SQLException {
                        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", DB_PASSWORD);
                        Statement stmt = conn.createStatement();
                        
                        // SQL Injection vulnerability
                        String query = "SELECT * FROM users WHERE id = '" + userInput + "'";
                        ResultSet rs = stmt.executeQuery(query);
                    }
                    
                    public void weakCrypto() throws Exception {
                        // Weak cryptographic algorithm
                        Cipher cipher = Cipher.getInstance("DES");
                    }
                    
                    public void commandInjection(String userInput) throws Exception {
                        // Command injection vulnerability  
                        Runtime.getRuntime().exec("ping " + userInput);
                    }
                }
                """;
    }

    /**
     * Code for testing custom rules
     */
    private static String getCustomRuleTestCode() {
        return """
                public class CustomRuleTest {
                    public void testMethod() {
                        String apiKey = "sk-1234567890abcdef"; // Should be caught by custom rule
                        String dbPassword = "supersecret"; // Should be caught by custom rule
                        
                        try {
                            // Should be caught by dangerous-exec rule
                            Runtime.getRuntime().exec("curl -X POST https://api.example.com");
                        } catch (Exception e) {
                            System.err.println("Error: " + e.getMessage());
                        }
                        
                        // Regular code that should not trigger rules
                        String message = "Hello World";
                        System.out.println(message);
                    }
                }
                """;
    }
}
