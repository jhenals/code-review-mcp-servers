package dev.jhenals.mcpsemgrep.utils;

import dev.jhenals.mcpsemgrep.model.domain.CodeFile;
import dev.jhenals.mcpsemgrep.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = FileUtils.class)
public class FileUtilsTest {

    private final FileUtils fileUtils= new FileUtils();

    private File tempFileToCleanup;
    private File tempDirToCleanup;

    private CodeFile codeFile = new CodeFile();
    private Map<String, Object> input;

    private static final String PARAM_NAME = "McpConfiguration";


    @AfterEach
    void cleanup() {
        if (tempFileToCleanup != null && tempFileToCleanup.exists()) {
            tempFileToCleanup.delete();
        }
        if (tempDirToCleanup != null && tempDirToCleanup.exists()) {
            // Delete directory recursively if needed
            deleteDirectoryRecursively(tempDirToCleanup);
        }
    }

    private void deleteDirectoryRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryRecursively(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    @Test
    void testCreateTemporaryFile() throws IOException, IOException {
        String fileName = "TestFile.java";
        String content = "public class TestFile {}";
        CodeFile codeFile = new CodeFile(fileName, content);

        File tempFile = fileUtils.createTemporaryFile(codeFile);
        tempFileToCleanup = tempFile;
        tempDirToCleanup = tempFile.getParentFile();

        assertNotNull(tempFile);
        assertTrue(tempFile.exists(), "Temporary file should exist");

        assertEquals(fileName, tempFile.getName(), "File name should match");

        String fileContent = Files.readString(tempFile.toPath());
        assertEquals(content, fileContent, "File content should match");

        File parentDir = tempFile.getParentFile();
        assertNotNull(parentDir);
        assertTrue(parentDir.exists());
        System.out.println(parentDir.getName());
        assertTrue(parentDir.getName().startsWith("semgrepTempDir"));
    }


    @Test
    void testCreateTemporaryRuleFile() throws IOException {
        String ruleContent = "rules:\n  - id: test-rule\n    pattern: 'test'";

        File tempRuleFile = fileUtils.createTemporaryRuleFile(ruleContent);
        tempFileToCleanup = tempRuleFile;

        assertNotNull(tempRuleFile);
        assertTrue(tempRuleFile.exists(), "Temporary rule file should exist");
        assertTrue(tempRuleFile.getName().startsWith("semgrep-rule"));
        assertTrue(tempRuleFile.getName().endsWith(".yaml"));

        String fileContent = Files.readString(tempRuleFile.toPath());
        assertEquals(ruleContent, fileContent, "Rule file content should match");
    }

    @Test
    void testCleanupTempFile_existingFile() throws IOException {
        File tempFile = Files.createTempFile("testCleanup", ".tmp").toFile();
        assertTrue(tempFile.exists());

        fileUtils.cleanupTempFile(tempFile);

        assertFalse(tempFile.exists(), "File should be deleted after cleanup");
    }

    @Test
    void testCleanupTempFile_nullFile() {
        assertDoesNotThrow(() -> fileUtils.cleanupTempFile(null));
    }

    @Test
    void testCleanupTempFile_nonExistentFile() {
        File nonExistentFile = new File("nonexistentfile.tmp");
        assertFalse(nonExistentFile.exists());

        assertDoesNotThrow(() -> fileUtils.cleanupTempFile(nonExistentFile));
    }
}
