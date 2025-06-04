package dev.jhenals.analyzer_server.tools;

import dev.jhenals.analyzer_server.models.*;
import dev.jhenals.analyzer_server.service.SecurityScannerService;
import dev.jhenals.analyzer_server.service.StaticAnalysisService;
import dev.jhenals.analyzer_server.service.TestCoverageService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ToolHandler {

    @Autowired
    private StaticAnalysisService staticAnalysisService;

    @Autowired
    private TestCoverageService testCoverageService;

    @Autowired
    private SecurityScannerService securityScannerService;

    @Tool(name = "analyze_code", description = "Performs static analysis of code")
    public List<Issue> analyzeCode(String input) throws IOException {
        return this.staticAnalysisService.analyzeCode(input);
    }

    @Tool(name = "analyze_test_coverage" , description = "Analyze test coverage reports")
    public TestCoverageResult analyzeTestCoverage(PRInput input) {
       return this.testCoverageService.analyzeTestCoverage(input);
    }

    @Tool(name = "security-scanner", description ="Scan for known security issues" )
    public SecurityScannerResult analyzeSecurityScanner(PRInput input){
        return this.securityScannerService.analyzeCode(input);
    }
}
