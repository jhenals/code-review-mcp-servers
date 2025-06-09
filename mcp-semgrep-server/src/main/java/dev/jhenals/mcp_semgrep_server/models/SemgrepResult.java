package dev.jhenals.mcp_semgrep_server.models;

import lombok.AllArgsConstructor;

//TODO: I can use a DP here : Wrapper Pattern

@AllArgsConstructor
public class SemgrepResult {
    private final boolean success;
    private final SemgrepScanResult output;
    private final SemgrepSecurityCheckResult securityCheckResult;
    private final String errorCode;
    private final String errorMessage;

    public static SemgrepResult scanSuccess(SemgrepScanResult output){
        return new SemgrepResult(true, output, null,null, null);
    }

    public static SemgrepResult securityCheckSuccess(SemgrepSecurityCheckResult securityCheckResult){
        return new SemgrepResult(true, null, securityCheckResult,null, null);
    }

    public static SemgrepResult error(String code, String errorMessage){
        return new SemgrepResult(false, null, null, code, errorMessage);
    }

}
