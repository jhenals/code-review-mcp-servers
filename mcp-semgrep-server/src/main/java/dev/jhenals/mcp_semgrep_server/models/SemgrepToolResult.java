package dev.jhenals.mcp_semgrep_server.models;

public record SemgrepToolResult(boolean success, SemgrepScanResult output,
                                SemgrepSecurityCheckResult securityCheckResult, String errorCode, String errorMessage) {

    public static SemgrepToolResult scanSuccess(SemgrepScanResult output) {
        return new SemgrepToolResult(true, output, null, null, null);
    }

    public static SemgrepToolResult securityCheckSuccess(SemgrepSecurityCheckResult securityCheckResult) {
        return new SemgrepToolResult(true, null, securityCheckResult, null, null);
    }

    public static SemgrepToolResult error(String code, String errorMessage) {
        return new SemgrepToolResult(false, null, null, code, errorMessage);
    }

    public String toString() {
        return "Semgrep Result: [" +
                "Success= " + success + "\n" +
                "Security Check Result= " + securityCheckResult + "\n" +
                "Semgrep Scan Result= " + output + "\n" +
                "Error Code= " + errorCode + "\n" +
                "Error Message= " + errorMessage + "]";
    }

}
