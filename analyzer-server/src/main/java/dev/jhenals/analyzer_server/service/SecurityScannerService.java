package dev.jhenals.analyzer_server.service;

import dev.jhenals.analyzer_server.models.SecurityScannerResult;
import dev.jhenals.analyzer_server.models.Commit;
import dev.jhenals.analyzer_server.models.PRInput;
import org.springframework.stereotype.Service;

@Service
public class SecurityScannerService {

    public SecurityScannerResult analyzeCode(PRInput input) {
        SecurityScannerResult result = new SecurityScannerResult();
        int numIssues = 0;

        for (Commit commit : input.commits) {
            if (commit.diff.contains("!= null") && !commit.diff.contains("isActive()")) {
                numIssues++;
                result.issues.add("Commit \"" + commit.message + "\": Consider checking both null and active state.");
            }

            if (commit.diff.contains("\"") && commit.message.contains("error message")) {
                numIssues++;
                result.issues.add("Commit \"" + commit.message + "\": Possible hardcoded string detected in error messages.");
            }

            if (!commit.message.matches("^(fix|feat|chore|refactor|test|docs): .*")) {
                numIssues++;
                result.issues.add("Commit \"" + commit.message + "\": Commit message should follow conventional commits format.");
            }
        }

        result.issues.add("Total issues found: " + numIssues);
        return result;
    }
}
