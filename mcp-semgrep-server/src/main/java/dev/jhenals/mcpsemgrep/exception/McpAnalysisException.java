package dev.jhenals.mcpsemgrep.exception;

import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class McpAnalysisException extends Exception{

    private final String code;
    private final String message;

    public McpAnalysisException(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public AnalysisResult handleError(String toolName, Exception e) {
        return AnalysisResult.builder()
                .errors(List.of("MCP_TOOL_ERROR: " + toolName + " - " + e.getMessage()))
                .build();
    }
}
