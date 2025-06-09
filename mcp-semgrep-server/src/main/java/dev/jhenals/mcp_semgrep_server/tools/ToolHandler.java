package dev.jhenals.mcp_semgrep_server.tools;

import dev.jhenals.mcp_semgrep_server.models.SemgrepResult;
import dev.jhenals.mcp_semgrep_server.service.SemgrepService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ToolHandler {

    @Autowired
    private SemgrepService semgrepService;

    @Tool(name = "semgrep_scan", description = "Performs general code scanning with configurable rulesets")
    public SemgrepResult semgrepScan(Map<String, Object> input){
        return this.semgrepService.semgrepScan(input);
    }





}
