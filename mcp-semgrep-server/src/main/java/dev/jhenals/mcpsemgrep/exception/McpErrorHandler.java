package dev.jhenals.mcpsemgrep.exception;

import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;

import java.util.List;

public class McpErrorHandler {
    public AnalysisResult handleError(String toolName, Exception e) {
        return AnalysisResult.builder()
                .errors(List.of("MCP_TOOL_ERROR: " + toolName + " - " + e.getMessage()))
                .build();
    }
}
