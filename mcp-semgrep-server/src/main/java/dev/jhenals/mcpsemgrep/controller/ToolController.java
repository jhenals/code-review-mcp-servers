package dev.jhenals.mcpsemgrep.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.model.domain.Finding;
import dev.jhenals.mcpsemgrep.model.request.CodeAnalysisRequest;
import dev.jhenals.mcpsemgrep.model.response.AnalysisResult;
import dev.jhenals.mcpsemgrep.service.analysis.SecurityAnalysisService;
import dev.jhenals.mcpsemgrep.service.analysis.CodeAnalysisService;
import dev.jhenals.mcpsemgrep.exception.McpAnalysisException;
import dev.jhenals.mcpsemgrep.service.semgrep.ConfigurationResolver;
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

    @Autowired
    private ConfigurationResolver configResolver;

    @Tool(name = "semgrep_scan",
            description = "Performs comprehensive static code analysis using Semgrep's rule engine."
    )
    public AnalysisResult performCodeAnalysis(
            @ToolParam(description = "Code file containing content and filename")
            CodeFile code_file,
            @ToolParam(description = "Analysis configuration")
            String config,
            @ToolParam(description = "Custom rule (optional)", required = false)
            String custom_rule
    ) throws McpAnalysisException, IOException {
        String resolvedConfig = configResolver.resolveGeneralConfig(config, code_file);
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(code_file)
                .config(resolvedConfig)
                .customRule(custom_rule)
                .build();
        return this.codeAnalysisService.analyzeCode(request);
    }

    @Tool(  name = "semgrep_scan_with_custom_rule",
            description = "Performs targeted static code analysis using user-provided custom Semgrep YAML rules."
    )
    public AnalysisResult performCodeAnalysisWithCustomRules(
            @ToolParam(description = "Code file containing content and filename")
            CodeFile code_file,
            @ToolParam(description = "Analysis configuration")
            String config,
            @ToolParam(description = "Custom rule (optional)", required = false)
            String custom_rule
    ) throws McpAnalysisException, IOException {

        String resolvedConfig = configResolver.resolveGeneralConfig(config, code_file);
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(code_file)
                .config(resolvedConfig)
                .customRule(custom_rule)
                .build();
        return this.codeAnalysisService.analyzeCodeWithCustomRules(request);
    }

    @Tool(  name = "security_check",
            description = "Performs rapid security-focused code analysis optimized for development workflows."
    )
    public AnalysisResult performSecurityCheck(
            @ToolParam(description = "Code file containing content and filename")
            CodeFile code_file,
            @ToolParam(description = "Analysis configuration")
            String config,
            @ToolParam(description = "Custom rule (optional)", required = false)
            String custom_rule
    ) throws McpAnalysisException, IOException {

        String resolvedConfig = configResolver.resolveGeneralConfig(config, code_file);
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
                .codeFile(code_file)
                .config(resolvedConfig)
                .customRule(custom_rule)
                .build();

        return this.securityAnalysisService.performSecurityAnalysis(request);
    }


}
