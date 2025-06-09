package dev.jhenals.mcp_semgrep_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SemgrepScanResult {

    @JsonProperty("version")
    private String version;

    @JsonProperty("results")
    private List<Map<String, Object>> results = new ArrayList<>();

    @JsonProperty("errors")
    private List<Map<String, Object>> errors = new ArrayList<>();

    @JsonProperty("paths")
    private Map<String, Object> paths = new HashMap<>();

    @JsonProperty("skipped_rules")
    private List<String> skippedRules = new ArrayList<>();

}
