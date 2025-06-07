package dev.jhenals.mcp_semgrep_server.tools;

import dev.jhenals.mcp_semgrep_server.service.SemgrepService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ToolHandler {

    @Autowired
    private SemgrepService semgrepService;

    @Tool(name = "analyze_code", description = "Performs static analysis of code")
    public String analyzeCode(String input) throws IOException {
        return this.semgrepService.analyzeCode(input);
    }

}
