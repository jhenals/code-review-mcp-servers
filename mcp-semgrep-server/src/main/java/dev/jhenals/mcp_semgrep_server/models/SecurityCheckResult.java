package dev.jhenals.mcp_semgrep_server.models;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class SecurityCheckResult {

    private final Map<String, String> securityCheckResult = new HashMap<>();

    public SecurityCheckResult(String message){
        this.securityCheckResult.put("message", message);
    }
}
