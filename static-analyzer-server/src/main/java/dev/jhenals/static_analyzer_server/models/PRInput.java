package dev.jhenals.static_analyzer_server.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PRInput {
    public String title;
    public String description;
    public List<Commit> commits;

    public PRInput(String title, String description, List<Commit> commits) {
        this.title= title;
        this.description= description;
        this.commits = commits;
    }
}