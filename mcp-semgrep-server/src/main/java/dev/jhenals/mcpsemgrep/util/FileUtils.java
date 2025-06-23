package dev.jhenals.mcpsemgrep.util;

import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


@Slf4j
@Component
public class FileUtils {

    public File createTemporaryFile(CodeFile codeFile) throws IOException {
        String tempDirPath = Files.createTempDirectory("semgrepTempDir").toString();
        File javaFile = new File(tempDirPath, codeFile.getFileName());
        javaFile.getParentFile().mkdirs();

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(javaFile), StandardCharsets.UTF_8))) {
            writer.write(codeFile.getContent());
        }

        log.debug("Created temporary file: {}", javaFile.getAbsolutePath());
        return javaFile;
    }

    public File createTemporaryRuleFile(String ruleContent) throws IOException {
        File tempFile = Files.createTempFile("semgrep-rule", ".yaml").toFile();
        Files.writeString(tempFile.toPath(), ruleContent, StandardCharsets.UTF_8);
        log.debug("Created temporary rule file: {}", tempFile.getAbsolutePath());
        return tempFile;
    }

    public void cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
                log.debug("Cleaned up temporary file: {}", file.getAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to cleanup temporary file: {}", file.getAbsolutePath(), e);
            }
        }
    }
}