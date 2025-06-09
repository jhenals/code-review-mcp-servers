package dev.jhenals.mcp_semgrep_server.models;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class SemgrepSecurityCheckResult {

    private Map<String, String> securityCheckResult = new HashMap<>();

    public SemgrepSecurityCheckResult(String message){
        this.securityCheckResult.put("message", message);
    }
}
