package dev.jhenals.analyzer_server.service;

import dev.jhenals.analyzer_server.models.TestCoverageResult;
import dev.jhenals.analyzer_server.models.Commit;
import dev.jhenals.analyzer_server.models.PRInput;
import org.springframework.stereotype.Service;

@Service
public class TestCoverageService {
    public TestCoverageResult analyzeTestCoverage(PRInput input) {
        TestCoverageResult result = new TestCoverageResult();
        int testRelatedChanges = 0;

        for (Commit commit : input.commits) {
            String diff = commit.diff.toLowerCase();

            boolean touchesTestFile = diff.contains("test") && diff.contains(".java");
            boolean containsAssertions = diff.contains("assert") || diff.contains("@test");
            boolean containsTestImports = diff.contains("org.junit") || diff.contains("mockito");

            if (touchesTestFile || containsAssertions || containsTestImports) {
                testRelatedChanges++;
                result.findings.add("Commit \"" + commit.message + "\": Test coverage indicators found in diff.");
            } else {
                result.findings.add("Commit \"" + commit.message + "\": No clear test-related code changes found.");
            }
        }

        result.summary = "Detected test-related changes in " + testRelatedChanges + " out of " + input.commits.size() + " commits.";
        return result;
    }
}
