package dev.jhenals.mcpsemgrep.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jhenals.mcpsemgrep.exception.McpErrorHandler;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.service.analysis.CodeAnalysisService;
import dev.jhenals.mcpsemgrep.service.analysis.SecurityAnalysisService;
import dev.jhenals.mcpsemgrep.service.semgrep.ConfigurationResolver;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SemgrepMcpHandler {

    private final CodeAnalysisService codeAnalysisService;
    private final SecurityAnalysisService securityAnalysisService;
    private final ConfigurationResolver configResolver;
    private final ObjectMapper objectMapper;
    private final McpErrorHandler errorHandler;  // ← 1. INJECTED AS DEPENDENCY


    public SemgrepMcpHandler(CodeAnalysisService codeAnalysisService,
                             SecurityAnalysisService securityAnalysisService,
                             ConfigurationResolver configResolver,
                             ObjectMapper objectMapper, McpErrorHandler errorHandler) {
        this.codeAnalysisService = codeAnalysisService;
        this.securityAnalysisService = securityAnalysisService;
        this.configResolver = configResolver;
        this.objectMapper = objectMapper;
        this.errorHandler= errorHandler;
    }

    // Return list of SyncToolSpecification instead of Tools
    public List<McpServerFeatures.SyncToolSpecification> getToolSpecifications() {
        return List.of(
                createSemgrepScanToolSpec(),
                createSemgrepScanWithCustomRuleToolSpec(),
                createSecurityCheckToolSpec()
        );
    }


    // Create SyncToolSpecification for semgrep_scan
    private McpServerFeatures.SyncToolSpecification createSemgrepScanToolSpec() {
        String schema = createSemgrepInputSchemaAsJson();

        var tool = new Tool(
                "semgrep_scan",
                "Performs comprehensive static code analysis using Semgrep's rule engine.",
                schema
        );

        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, arguments) -> {
                    try {
                        log.info("Executing semgrep_scan with arguments: {}", arguments);
                        CodeAnalysisRequest request = parseAnalysisRequest(arguments);
                        AnalysisResult result = codeAnalysisService.analyzeCode(request);
                        return errorHandler.createSuccessResult(result);
                    } catch (Exception e) {
                        log.error("Error in semgrep_scan", e);
                        return errorHandler.handleError("semgrep_scan", e);
                    }
                }
        );
    }

    // Create SyncToolSpecification for semgrep_scan_with_custom_rule
    private McpServerFeatures.SyncToolSpecification createSemgrepScanWithCustomRuleToolSpec() {
        String schema = createSemgrepInputSchemaAsJson();

        var tool = new Tool(
                "semgrep_scan_with_custom_rule",
                " Performs targeted static code analysis using user-provided custom Semgrep YAML rules.",
                schema
        );

        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, arguments) -> {
                    try {
                        log.info("Executing semgrep_scan_with_custom_rule with arguments: {}", arguments);
                        CodeAnalysisRequest request = parseAnalysisRequest(arguments);
                        AnalysisResult result = codeAnalysisService.analyzeCodeWithCustomRules(request);
                        return errorHandler.createSuccessResult(result);
                    } catch (Exception e) {
                        log.error("Error in semgrep_scan_with_custom_rule", e);
                        return errorHandler.handleError("semgrep_scan_with_custom_rule", e);
                    }
                }
        );
    }

    // Create SyncToolSpecification for security_check
    private McpServerFeatures.SyncToolSpecification createSecurityCheckToolSpec() {
        String schema = createSemgrepInputSchemaAsJson();

        var tool = new Tool(
                "security_check",
                "Performs rapid security-focused code analysis optimized for development workflows.",
                schema
        );

        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, arguments) -> {
                    try {
                        log.info("Executing security_check with arguments: {}", arguments);
                        CodeAnalysisRequest request = parseAnalysisRequest(arguments);
                        AnalysisResult result = securityAnalysisService.performSecurityAnalysis(request);
                        return errorHandler.createSuccessResult(result);

                    } catch (Exception e) {
                        log.error("Error in security_check", e);
                        return errorHandler.handleError("security_check", e);
                    }
                }
        );
    }

    // Convert your Map-based schema to JSON string
    private String createSemgrepInputSchemaAsJson() {
        try {
            Map<String, Object> schema = createSemgrepInputSchema();
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            log.error("Failed to serialize schema to JSON", e);
            throw new RuntimeException("Schema serialization failed", e);
        }
    }

    private Map<String, Object> createSemgrepInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "code_file", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "filename", Map.of("type", "string"),
                                        "content", Map.of("type", "string")
                                ),
                                "required", List.of("content"),
                                "description", "Code file containing content and filename"
                        ),
                        "config", Map.of(
                                "type", "string",
                                "description", "Analysis configuration"
                        ),
                        "custom_rule", Map.of(
                                "type", "string",
                                "description", "Custom rule (optional)"
                        )
                ),
                "required", List.of("code_file", "config")
        );
    }

    // Tool handler methods (keep your existing logic)
    private CallToolResult handleSemgrepScan(Map<String, Object> arguments) {
        try {
            var request = parseAnalysisRequest(arguments);
            var result = codeAnalysisService.analyzeCode(request);

            return new CallToolResult(
                    List.of(new TextContent(objectMapper.writeValueAsString(result))),
                    false
            );
        } catch (Exception e) {
            return createErrorResult("semgrep_scan", e);
        }
    }

    private CallToolResult handleSemgrepScanWithCustomRule(Map<String, Object> arguments) {
        try {
            var request = parseAnalysisRequest(arguments);
            var result = codeAnalysisService.analyzeCodeWithCustomRules(request);

            return new CallToolResult(
                    List.of(new TextContent(objectMapper.writeValueAsString(result))),
                    false
            );
        } catch (Exception e) {
            return createErrorResult("semgrep_scan_with_custom_rule", e);
        }
    }

    private CallToolResult handleSecurityCheck(Map<String, Object> arguments) {
        try {
            var request = parseAnalysisRequest(arguments);
            var result = securityAnalysisService.performSecurityAnalysis(request);

            return new CallToolResult(
                    List.of(new TextContent(objectMapper.writeValueAsString(result))),
                    false
            );
        } catch (Exception e) {
            return createErrorResult("security_check", e);
        }
    }

    private CodeAnalysisRequest parseAnalysisRequest(Map<String, Object> arguments) throws Exception {
        // Validate required arguments
        if (!arguments.containsKey("code_file")) {
            throw new IllegalArgumentException("Missing required argument: code_file");
        }
        if (!arguments.containsKey("config")) {
            throw new IllegalArgumentException("Missing required argument: config");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> codeFileMap = (Map<String, Object>) arguments.get("code_file");

        if (codeFileMap == null || !codeFileMap.containsKey("content")) {
            throw new IllegalArgumentException("code_file must contain 'content' field");
        }

        CodeFile codeFile = objectMapper.convertValue(codeFileMap, CodeFile.class);
        String config = (String) arguments.get("config");
        String customRule = (String) arguments.get("custom_rule");

        String resolvedConfig = configResolver.resolveGeneralConfig(config, codeFile);

        return CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .config(resolvedConfig)
                .customRule(customRule)
                .build();
    }

    private CallToolResult createErrorResult(String toolName, Exception e) {
        String errorMessage = String.format("MCP_TOOL_ERROR: %s - %s", toolName, e.getMessage());
        return new CallToolResult(
                List.of(new TextContent(errorMessage)),
                true
        );
    }
}

