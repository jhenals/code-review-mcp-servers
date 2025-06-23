package dev.jhenals.mcpsemgrep.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class McpAnalysisException extends Exception{

    private final String code;
    private final String message;

    public McpAnalysisException(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
