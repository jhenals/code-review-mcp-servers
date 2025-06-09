package dev.jhenals.mcp_semgrep_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class CodeWithLanguage {
    @JsonProperty("language")
    private String language = "java";

    @JsonProperty("content")
    private String  content;
}
