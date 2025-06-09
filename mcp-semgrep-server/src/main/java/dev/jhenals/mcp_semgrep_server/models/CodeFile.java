package dev.jhenals.mcp_semgrep_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeFile {
    @JsonProperty("filename")
    private String fileName;

    @JsonProperty("content")
    private String content;


    public String getFilename() {
        return fileName;
    }
}
