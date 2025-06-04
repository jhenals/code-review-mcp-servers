package dev.jhenals.analyzer_server.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
public class TestCoverageResult {
    public List<String> findings = new ArrayList<>();
    public String summary;
}
