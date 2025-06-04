package dev.jhenals.analyzer_server.models;

import java.util.ArrayList;
import java.util.List;

public class SecurityScannerResult {
    public List<String> issues;

    public SecurityScannerResult(){
        this.issues= new ArrayList<>();
    }

}
