package dev.jhenals.mcp_semgrep_server.tools;

import dev.jhenals.mcp_semgrep_server.models.CodeWithLanguage;
import dev.jhenals.mcp_semgrep_server.models.SemgrepResult;
import dev.jhenals.mcp_semgrep_server.service.SemgrepService;
import dev.jhenals.mcp_semgrep_server.utils.McpError;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ToolHandler {

    @Autowired
    private SemgrepService semgrepService;

    @Tool(name = "semgrep_scan", description = "Performs general code scanning with configurable rulesets")
    public SemgrepResult semgrepScan(Map<String, Object> input){
        return this.semgrepService.semgrepScan(input);
    }

    @Tool(name = "semgrep_scan_with_custom_result", description = "Performs code scanning with user-provided YAML rules")
    public SemgrepResult semgrepScanWithCustomResult(Map<String,Object> input){
        return this.semgrepService.semgrepScanWithCustomRule(input);
    }

    @Tool(name = "security_check", description = "Performs a quick security-focused scan with formatted output")
    public SemgrepResult securityCheck(Map<String,Object> input){
        return this.semgrepService.securityCheck(input);
    }

    @Tool(name = "get_supported_language", description = "Returns lists of Semgrep-supported programming languages")
    public List<String> securityCheck() throws McpError {
        return this.semgrepService.getSupportedLanguages();
    }


    @Tool(name="get_abstract_syntax_tree", description = "Returns AST representation of code")
    public Map<String, String> getAbstractSyntaxTree(CodeWithLanguage code){
        return this.semgrepService.getAbstractSyntaxTree(code);
    }

    @Tool(name = "semgrep_rule_schema", description = "Retrieves the JSON schema for writing Semgrep rules")
    public Map<String, String> getSemgrepRuleSchema(String ruleId){
        return this.semgrepService.getSemgrepRuleSchema(ruleId);
    }





}
