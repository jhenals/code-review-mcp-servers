package dev.jhenals.mcpsemgrep.controller;

import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.service.analysis.SecurityAnalysisService;
import dev.jhenals.mcpsemgrep.service.analysis.CodeAnalysisService;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ToolController {

    @Autowired
    private CodeAnalysisService codeAnalysisService;

    @Autowired
    private SecurityAnalysisService securityAnalysisService;

    @Tool(name = "semgrep_scan", description = "Performs general code scanning with configurable rulesets")
    public AnalysisResult performCodeAnalysis(CodeAnalysisRequest request) throws McpAnalysisException, IOException {
        return this.codeAnalysisService.analyzeCode(request);
    }

    @Tool(name = "semgrep_scan_with_custom_rule", description = "Performs code scanning with user-provided YAML rules")
    public AnalysisResult performCodeAnalysisWithCustomRules(CodeAnalysisRequest request) throws McpAnalysisException, IOException {
        return this.codeAnalysisService.analyzeCodeWithCustomRules(request);
    }

    @Tool(name = "security_check", description = "Performs a quick security-focused scan with formatted output")
    public AnalysisResult performSecurityCheck(CodeAnalysisRequest request) throws McpAnalysisException, IOException {
        return this.securityAnalysisService.performSecurityAnalysis(request);
    }


}
