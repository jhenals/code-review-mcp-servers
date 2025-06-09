package dev.jhenals.mcp_semgrep_server.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class McpError extends Exception{

    private final String code;
    private final String message;

    public McpError(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
