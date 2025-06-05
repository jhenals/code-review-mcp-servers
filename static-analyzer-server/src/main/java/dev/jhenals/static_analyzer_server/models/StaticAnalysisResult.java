package dev.jhenals.static_analyzer_server.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class StaticAnalysisResult {
    public List<Issue> issues;

    public StaticAnalysisResult() {
        this.issues = new ArrayList<>();
    }

    public String toString(){
        return this.issues.stream()
                .map(Issue::toString)
                .collect(Collectors.joining("\n"));
    }


}
