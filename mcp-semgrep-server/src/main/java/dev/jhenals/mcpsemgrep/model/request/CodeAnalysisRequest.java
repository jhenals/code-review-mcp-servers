package dev.jhenals.mcpsemgrep.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class CodeAnalysisRequest {

    @NonNull
    @JsonProperty("code_file")
    private final CodeFile codeFile;

    private final String config;

    @JsonProperty("custom_rule")
    private final String customRule;

    public static CodeAnalysisRequest forAutoConfig(CodeFile codeFile) {
        return CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .config("auto")
                .build();
    }

    public static CodeAnalysisRequest forCustomRule(CodeFile codeFile, String rule) {
        return CodeAnalysisRequest.builder()
                .codeFile(codeFile)
                .customRule(rule)
                .build();
    }
}
