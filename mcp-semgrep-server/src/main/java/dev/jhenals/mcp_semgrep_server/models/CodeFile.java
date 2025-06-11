package dev.jhenals.mcp_semgrep_server.models;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeFile {
    private String fileName;
    private String content;
}
