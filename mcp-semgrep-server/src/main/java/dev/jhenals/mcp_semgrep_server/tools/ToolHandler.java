package dev.jhenals.mcp_semgrep_server.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.jhenals.mcp_semgrep_server.models.SemgrepToolResult;
import dev.jhenals.mcp_semgrep_server.models.StaticAnalysisResult;
import dev.jhenals.mcp_semgrep_server.service.SecurityCheckService;
import dev.jhenals.mcp_semgrep_server.service.StaticAnalysisService;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Component
public class ToolHandler {

    @Autowired
    private StaticAnalysisService staticAnalysisService;

    @Autowired
    private SecurityCheckService securityCheckService;

    @Tool(name = "semgrep_scan", description = "Performs general code scanning with configurable rulesets")
    public StaticAnalysisResult semgrepScan( Map<String, Object> input) throws McpError {
        log.info(">>>semgrepScan input{}", input);
        return this.staticAnalysisService.semgrepScan(input);
    }

    @Tool(name = "semgrep_scan_with_custom_rule", description = "Performs code scanning with user-provided YAML rules")
    public StaticAnalysisResult semgrepScanWithCustomRule(Map<String,Object> input) throws McpError {
        return this.staticAnalysisService.semgrepScanWithCustomRule(input);
    }

    @Tool(name = "security_check", description = "Performs a quick security-focused scan with formatted output")
    public StaticAnalysisResult securityCheck(Map<String,Object> input) throws McpError {
        return this.securityCheckService.securityCheck(input);
    }


}
