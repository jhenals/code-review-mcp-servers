package dev.jhenals.mcpsemgrep.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.model.domain.Finding;
import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.service.analysis.SecurityAnalysisService;
import dev.jhenals.mcpsemgrep.service.analysis.CodeAnalysisService;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(name = "semgrep_scan",
            description = """
                Performs comprehensive static code analysis using Semgrep's rule engine.
                Analyzes code for security vulnerabilities, code quality issues, and best practices.
                Supports configurable rulesets including OWASP, CWE, and custom security patterns.
                Returns structured findings with severity levels, locations, and remediation guidance.
                """
    )
    public AnalysisResult performCodeAnalysis(
            @ToolParam(description = "Code analysis request containing the code file and configuration")
            CodeAnalysisRequest request
    ) throws McpAnalysisException, IOException {
        return this.codeAnalysisService.analyzeCode(request);
    }

    @Tool(  name = "semgrep_scan_with_custom_rule",
            description = """
                Performs targeted static code analysis using user-provided custom Semgrep YAML rules.
                Enables organization-specific security policies and proprietary framework analysis.
                Supports custom pattern matching, severity levels, and compliance requirements.
                Returns analysis results based on custom rule definitions with detailed violations.
                """
    )
    public AnalysisResult performCodeAnalysisWithCustomRules(
            @ToolParam(description = "Code analysis request containing the code file and configuration")
            CodeAnalysisRequest request
    ) throws McpAnalysisException, IOException {
        return this.codeAnalysisService.analyzeCodeWithCustomRules(request);
    }


    @Tool(  name = "security_check",
            description = """
                Performs rapid security-focused code analysis optimized for development workflows.
                Targets high-impact vulnerabilities including injection flaws, XSS, and crypto weaknesses.
                Provides fast execution with reduced false positives and actionable remediation guidance.
                Returns prioritized security findings with OWASP/CWE classifications and risk assessments.
                """

    )
    public AnalysisResult performSecurityCheck(
            @ToolParam(description = "Code analysis request containing the code file and configuration")
            CodeAnalysisRequest request
    ) throws McpAnalysisException, IOException {
        return this.securityAnalysisService.performSecurityAnalysis(request);
    }


}
