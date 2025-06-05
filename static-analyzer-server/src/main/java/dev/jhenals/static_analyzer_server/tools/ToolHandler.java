package dev.jhenals.static_analyzer_server.tools;

import dev.jhenals.static_analyzer_server.models.*;
import dev.jhenals.static_analyzer_server.service.StaticAnalysisService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ToolHandler {

    @Autowired
    private StaticAnalysisService staticAnalysisService;

    @Tool(name = "analyze_code", description = "Performs static analysis of code")
    public String analyzeCode(String input) throws IOException {
        return this.staticAnalysisService.analyzeCode(input);
    }

}
