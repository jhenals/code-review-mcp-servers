package dev.jhenals.analyzer_server.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StaticAnalysisResult {
    public List<String> issues;

    public StaticAnalysisResult() {
        this.issues = new ArrayList<>();
    }
}
