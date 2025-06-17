package dev.jhenals.mcp_semgrep_server.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeFile {
    @JsonProperty("fileName")
    private String fileName;
    @JsonProperty("content")
    private String content;
}
